package org.elkd.core.runtime.client.controller

import io.grpc.stub.StreamObserver
import org.elkd.core.server.client.RpcClientRequest
import org.elkd.core.server.client.RpcClientResponse

/**
 * Executes client availableCommandIds received from client connections
 */
class ClientCommandHandler(
    private val syslogCommandExecutor: SyslogCommandExecutor,
    private val transformers: Map<SyslogCommandType, CommandParser>
) {

  fun handle(request: RpcClientRequest, response: StreamObserver<RpcClientResponse>) {
    when (request.command) {
      in SyslogCommandType.availableCommandIds -> syslogCommandHandler(request, response)
      else -> {
        returnError(response, "unknown command")
      }
    }
  }

  private fun returnError(response: StreamObserver<RpcClientResponse>, message: String) {
    response.onNext(RpcClientResponse.newBuilder()
        .setResponse(message).build())
    response.onCompleted()
  }

  private fun syslogCommandHandler(request: RpcClientRequest, response: StreamObserver<RpcClientResponse>) {
    val clientRequest = ClientSyslogCommandPack(
        command = buildSyslogCommand(request),
        onComplete = {
          response.onNext(RpcClientResponse.newBuilder()
              .setResponse(CLIENT_SUCCESS_RESPONSE).build())
          response.onCompleted()
        },
        onError = { message ->
          response.onNext(RpcClientResponse.newBuilder()
              .setResponse(message).build())
          response.onCompleted()
        }
    )

    syslogCommandExecutor.execute(clientRequest)
  }

  private fun buildSyslogCommand(request: RpcClientRequest): SyslogCommand {
    val type = SyslogCommandType.fromId(request.command)
    return transformers.getOrDefault(type, NoOpCommandParser())(type, request)
  }

  private companion object {
    const val CLIENT_SUCCESS_RESPONSE = "ok"
  }
}
