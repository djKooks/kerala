package org.elkd.core.log.ds

import org.elkd.core.log.CommitResult
import org.elkd.core.log.LogEntry

interface Log<E : LogEntry> {
  val id: String
  val commitIndex: Long
  val lastIndex: Long
  val lastEntry: E

  fun append(entry: E): Long

  fun append(index: Long, entry: E): Long

  fun read(index: Long): E?

  fun read(from: Long, to: Long): List<E>

  fun commit(index: Long): CommitResult<E>

  fun revert(index: Long)
}