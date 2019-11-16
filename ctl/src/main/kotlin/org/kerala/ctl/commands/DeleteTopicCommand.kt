package org.kerala.ctl.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import org.kerala.core.server.client.RpcArgPair
import org.kerala.ctl.Context
import org.kerala.ctl.sendCommand
import org.kerala.shared.client.ClientACK

class DeleteTopicCommand : CliktCommand(name = "delete-topic") {
  val namespace by argument("namespace", help = "namespace of the new topic")

  override fun run() {
    try {
      val response = sendCommand(Context.channel!!, "delete-topic", listOf(
          RpcArgPair.newBuilder()
              .setArg("namespace")
              .setParam(namespace)
              .build())
      )
      when (response.status) {
        ClientACK.Codes.OK.id -> echo(response.response)
        ClientACK.Codes.ERROR.id -> throw Exception("`delete-topic` call failed -> ${response.response}")
      }
    } catch (e: Exception) {
      echo(e.message, err = true)
    }
  }
}