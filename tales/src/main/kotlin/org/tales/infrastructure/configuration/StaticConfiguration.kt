package org.tales.infrastructure.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.tales.infrastructure.image.ImageType

@Configuration
class StaticConfiguration {
  @Bean("thumbnailType")
  fun thumbnailType() = ImageType.JPEG

  @Bean("pdfImageType")
  fun pdfImageType() = ImageType.JPEG

  @Bean("pdfResolution")
  fun pdfResolution(): Float = 3200F
}
