package org.elkd.core.runtime.client.command

import java.util.UUID
import org.elkd.core.consensus.OpCategory

data class ClientCommandPack(
    val requestId: String = UUID.randomUUID().toString(),
    val opCategory: OpCategory = OpCategory.WRITE,
    val onComplete: (message: String) -> Unit,
    val onError: (error: String) -> Unit,
    val timeout: Int = 3000,
    val command: ClientCommand
)
