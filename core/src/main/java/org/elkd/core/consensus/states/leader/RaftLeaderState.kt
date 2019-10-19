package org.elkd.core.consensus.states.leader

import io.grpc.stub.StreamObserver
import org.elkd.core.consensus.OpCategory
import org.elkd.core.consensus.Raft
import org.elkd.core.consensus.messages.AppendEntriesRequest
import org.elkd.core.consensus.messages.AppendEntriesResponse
import org.elkd.core.consensus.messages.RequestVoteRequest
import org.elkd.core.consensus.messages.RequestVoteResponse
import org.elkd.core.consensus.replication.Replicator
import org.elkd.core.consensus.states.RaftState
import org.elkd.core.log.LogChangeReason
import org.elkd.core.log.commands.AppendCommand
import org.elkd.core.runtime.client.command.ClientCommand
import org.elkd.core.runtime.client.command.ClientCommandType

class RaftLeaderState(private val raft: Raft) : RaftState {
  private var replicator: Replicator? = null

  override fun on() {
    /* for test sake, append a new entry to the logger here so we have something to replicate */
    broadcastConsensusInformation()
    replicator = Replicator(raft).apply {
      launch()
    }
  }

  override fun off() {
    /* Force-shutdown the replication process - we must honor the transition */
    replicator?.shutdown()
  }

  override val supportedOps = setOf(OpCategory.WRITE, OpCategory.READ)

  override fun delegateAppendEntries(
      request: AppendEntriesRequest,
      stream: StreamObserver<AppendEntriesResponse>
  ) {
    with(stream) {
      onNext(AppendEntriesResponse(raft.raftContext.currentTerm, false))
      onCompleted()
    }
  }

  override fun delegateRequestVote(
      request: RequestVoteRequest,
      stream: StreamObserver<RequestVoteResponse>
  ) {
    /* If term > currentTerm, Raft will always transitionRequest to Follower state. model received
       here will only be term <= currentTerm so we can defer all logic to the consensus delegate.
     */
    with(stream) {
      onNext(RequestVoteResponse(raft.raftContext.currentTerm, false))
      onCompleted()
    }
  }

  private fun broadcastConsensusInformation() {
    raft.topicModule.syslog.logFacade.commandExecutor.execute(AppendCommand.build(
        ClientCommand.builder(ClientCommandType.CONSENSUS_CHANGE) {
          arg("leaderNode", raft.clusterSet.selfNode.id)
        }.asEntry(raft.raftContext.currentTerm),
        LogChangeReason.REPLICATION
    ))
  }
}
