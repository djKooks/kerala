package org.elkd.core.consensus;

import com.google.common.base.Preconditions;
import io.grpc.stub.StreamObserver;
import org.elkd.core.consensus.payload.AppendEntriesRequest;
import org.elkd.core.consensus.payload.AppendEntriesResponse;
import org.elkd.core.consensus.payload.RequestVotesRequest;
import org.elkd.core.consensus.payload.RequestVotesResponse;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

public class LeaderDelegate implements Delegate {
  private static final Logger LOG = Logger.getLogger(CandidateDelegate.class.getName());
  private final Consensus mConsensus;

  LeaderDelegate(@Nonnull final Consensus consensus) {
    mConsensus = Preconditions.checkNotNull(consensus, "consensus");
  }

  @Override
  public void on() {
    LOG.info("online");
  }

  @Override
  public void off() {
    LOG.info("offline");
  }

  @Override
  public AppendEntriesResponse delegateAppendEntries(final AppendEntriesRequest request,
                                                     final StreamObserver<AppendEntriesResponse> response) {
    return null;
  }

  @Override
  public RequestVotesResponse delegateRequestVotes(final RequestVotesRequest request,
                                                   final StreamObserver<RequestVotesResponse> response) {
    return null;
  }
}
