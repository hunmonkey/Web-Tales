package org.tales.infrastructure.configuration

sealed class SettingChangedEvent {
  data object TaskPoolSize : SettingChangedEvent()
}
