package org.elkd.core.consensus.election

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.log4j.Logger
import org.elkd.core.consensus.messages.RequestVoteRequest
import org.elkd.core.consensus.messages.RequestVoteResponse
import org.elkd.core.server.cluster.ClusterMessenger
import org.elkd.core.server.cluster.Node
import kotlin.coroutines.CoroutineContext

/**
 * Performs an election across the cluster, requesting votes and tallying responses.
 *
 * @param voteRequest The request to use to request votes from cluster.
 * @param electionStrategy The election strategy to use to count the votes and determine outcome.
 * @param onSuccess Election success callback
 * @param onFailure Election fail callback
 * @param clusterMessenger Messenger mechanism, contains logical cluster to perform election across.
 */
class ElectionScheduler private constructor(private val voteRequest: RequestVoteRequest,
                                            private val electionStrategy: ElectionStrategy,
                                            private var onSuccess: () -> Unit,
                                            private var onFailure: () -> Unit,
                                            private val clusterMessenger: ClusterMessenger): CoroutineScope {
  val job: Job
    get() = Job()
  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.IO

  private var scheduled = false
  private var finished = false
  private var electionTally: ElectionTally = ElectionTally(clusterMessenger.clusterSet.allNodes.size)

  fun schedule() {
    if (scheduled) return
    LOG.info("Scheduling a new election with $voteRequest")

    scheduled = true

    /* Vote for self */
    handleVoteResponse(clusterMessenger.clusterSet.localNode, true)

    /* dispatch votes across cluster */
    launch(coroutineContext) { dispatchVoteRequest() }
  }

  fun finish() {
    finished = true
  }

  private suspend fun dispatchVoteRequest() {
    clusterMessenger.clusterSet.nodes.forEach {
      val response = clusterMessenger.dispatch<RequestVoteResponse>(it, voteRequest)
      handleVoteResponse(it, response?.isVoteGranted)
    }
  }

  private fun handleVoteResponse(node: Node, isVoteGranted: Boolean?) {
    try {
      when (isVoteGranted) {
        true -> electionTally.recordUpVote(node.id)
        false -> electionTally.recordDownVote(node.id)
      }
      postVoteCheck()
    } catch (e: Exception) { }
  }

  private fun postVoteCheck() {
    if (!finished && electionStrategy.isComplete(electionTally)) {
      when (electionStrategy.isSuccessful(electionTally)) {
        true -> {
          LOG.info("Successful election $electionTally")
          onSuccess()
        }
        false -> {
          LOG.info("Unsuccessful election $electionTally")
          onFailure()
        }
      }
      finish()
    }
  }

  companion object {
    private val LOG = Logger.getLogger(ElectionScheduler::class.java.name)
    @JvmStatic fun create(voteRequest: RequestVoteRequest,
                          onSuccess: () -> Unit,
                          onFailure: () -> Unit,
                          clusterMessenger: ClusterMessenger): ElectionScheduler {
      return ElectionScheduler(
          voteRequest,
          /* Raft uses majority, so we'll just hard-code this strategy for now. */
          MajorityElectionStrategy(),
          onSuccess,
          onFailure,
          clusterMessenger
      )
    }
  }
}