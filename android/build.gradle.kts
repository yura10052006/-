// Кореневий build-файл. Плагіни оголошуємо тут (apply false), застосовуємо в :app.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
