package org.elkd.core.consensus

import org.elkd.core.client.model.OperationCategory
import org.elkd.core.config.Config
import org.elkd.core.consensus.messages.Entry
import org.elkd.core.consensus.messages.Request
import org.elkd.core.log.Log
import org.elkd.core.log.LogCommandExecutor
import org.elkd.core.log.LogComponentProvider
import org.elkd.core.server.cluster.ClusterMessenger
import org.elkd.core.server.cluster.ClusterSet
import org.elkd.shared.annotations.Mockable

/**
 * Raft module performs consensus of the logComponentProvider over the cluster, using the [ClusterMessenger]
 * as a means of communication. This module registers as a delegate to the [org.elkd.core.server.cluster.ClusterService]
 * and acts as a state machine between the various consensus states.
 *
 * @see [https://raft.github.io/raft.pdf](https://raft.github.io/raft.pdf)
 * @see RaftFollowerState
 * @see RaftCandidateState
 * @see RaftLeaderState
 */
@Mockable
class Raft
internal constructor(val config: Config,
                     val clusterMessenger: ClusterMessenger,
                     val raftContext: RaftContext,
                     val logComponentProvider: LogComponentProvider<Entry>) {

  val log: Log<Entry>
    get() = logComponentProvider.log

  val logCommandExecutor: LogCommandExecutor<Entry>
    get() = logComponentProvider.logCommandExecutor

  val clusterSet: ClusterSet
    get() = clusterMessenger.clusterSet

  val supportedOperations: Set<OperationCategory>
    get() = delegator.supportedOperations

  val delegator: RaftDelegator = RaftDelegator(DefaultStateFactory(this), listOf(
      object: TransitionContract {

        override fun isTransitionRequired(request: Request): Boolean {
          return request.term > raftContext.currentTerm
        }

        override val transitionTo: State
          get() = State.FOLLOWER

        override val transitionPreHook: (request: Request) -> Unit
          get() = { request ->
            raftContext.currentTerm = request.term
            raftContext.votedFor = null
          }

        override val transitionPostHook: (request: Request) -> Unit
          get() = { /* no op */ }
      }
  ))

  fun initialize() {
    delegator.transition(State.FOLLOWER)
  }
}
