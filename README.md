# Gacha Codes — Genshin Impact + Wuthering Waves

Красивое Android-приложение с двумя вкладками, автоматическим обновлением промокодов,
уведомлениями и копированием кода одним нажатием.

## Сборка APK через Termux

### 1. Установи Termux
Рекомендуется брать Termux из F-Droid или официального GitHub-релиза проекта.

### 2. Подготовь окружение
Открой Termux:

```bash
pkg update -y
pkg upgrade -y
pkg install git wget unzip openjdk-17 -y
```

Проверь Java:

```bash
java -version
```

### 3. Распакуй проект
Если ZIP находится в папке Download:

```bash
termux-setup-storage
cd ~/storage/downloads
unzip GachaCodesApp.zip -d GachaCodesApp
cd GachaCodesApp
```

### 4. Установи Gradle
В Termux можно установить Gradle из репозитория:

```bash
pkg install gradle -y
gradle --version
```

Если версия Gradle слишком старая для Android Gradle Plugin, проще использовать
Android Studio на ПК или скачать совместимый Gradle дистрибутив.

### 5. Первый запуск сборки

```bash
gradle assembleDebug
```

После успешной сборки APK будет здесь:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Установить его можно так:

```bash
termux-open app/build/outputs/apk/debug/app-debug.apk
```

Если Android спросит разрешение на установку неизвестных приложений — разреши установку
для приложения, через которое открываешь APK.

## 6. Настрой автоматические промокоды

В `MainActivity.kt` найди:

```text
https://YOUR_GITHUB_USERNAME.github.io/gacha-codes/codes.json
```

и замени на URL своего GitHub Pages.

Например:

```text
https://myname.github.io/gacha-codes/codes.json
```

После изменения снова:

```bash
gradle assembleDebug
```

## 7. Что делает приложение

- Genshin Impact / Wuthering Waves — отдельные вкладки.
- Кнопка копирования справа от каждого кода.
- После копирования появляется «✓ Скопировано».
- Кнопка обновления запускает проверку.
- WorkManager проверяет новые коды в фоне.
- При появлении новых кодов приходит уведомление.
- GitHub Action может автоматически обновлять `codes.json`.

## Важный момент

Для публикации полноценной версии лучше сделать отдельную базу с полями:

`game`, `code`, `reward`, `expires`, `active`, `source`, `seen`.

Тогда приложение сможет показывать только действительно активные коды,
отмечать просроченные и отображать награды.
