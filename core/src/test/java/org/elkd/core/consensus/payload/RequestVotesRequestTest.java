package org.elkd.core.consensus.payload;

import org.elkd.core.consensus.messages.RequestVotesRequest;
import org.junit.Test;

import static org.junit.Assert.*;

public class RequestVotesRequestTest {
  private static final int TERM = 0;
  private static final int CANDIDATE_ID = 1;
  private static final long LAST_LOG_INDEX = 2;
  private static final int LAST_LOG_TERM = 3;

  @Test
  public void should_build_with_properties() {
    // Given / When
    final RequestVotesRequest request = RequestVotesRequest.builder(
        TERM,
        CANDIDATE_ID,
        LAST_LOG_INDEX,
        LAST_LOG_TERM
    )
        .build();

    // Then
    assertEquals(TERM, request.getTerm());
    assertEquals(CANDIDATE_ID, request.getCandidateId());
    assertEquals(LAST_LOG_INDEX, request.getLastLogIndex());
    assertEquals(LAST_LOG_TERM, request.getLastLogTerm());
  }
}
