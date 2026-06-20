package org.tales.interfaces.sse.dto

data class TaskQueueSseDto(
  val count: Int,
  val countByType: Map<String, Int>,
)
