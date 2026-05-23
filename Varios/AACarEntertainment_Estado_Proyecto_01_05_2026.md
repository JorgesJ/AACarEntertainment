# AACarEntertainment — Estado del Proyecto

## ⚠️ REGLAS CRÍTICAS PARA EL NUEVO CHAT
- **NO refactorizar** ningún archivo existente sin motivo explícito
- **NO cambiar** la arquitectura HubActivity + CAR_LAUNCHER
- **NO tocar** los archivos Java: `HomeActivity.java`, `IptvActivity.java`, `MusicActivity.java`, `RadioActivity.java`, `MirrorActivity2.java`, `SettingsActivity.java`
- **NO eliminar** módulos existentes sin confirmación
- Hacer **backup** antes de cualquier cambio importante
- Los regex de PowerShell fallan frecuentemente — preferir edición directa en Android Studio o reescritura completa del archivo

---

## DATOS DEL PROYECTO

**Package:** `com.jorso.carapp`
**Ruta:** `C:\Users\jorso\Documents\AACarEntertainment\`
**Keystore:** `aacarentertainment.jks` | alias: `aacarentertainment` | pass: `aacar2026`
**ADB device:** `dfaf6ffa` (Xiaomi POCO X6 5G, Android 16, HyperOS/MIUI)
**Play Console:** Prueba interna activa | tester: jorsorju@gmail.com
**versionCode actual:** 23 (subido a Play Console prueba interna)

---

## SCRIPT DE INSTALACIÓN

`instalar.ps1` en raíz del proyecto:
```powershell
.\gradlew assembleRelease
adb -s dfaf6ffa uninstall com.jorso.carapp
adb -s dfaf6ffa install app\build\outputs\apk\release\app-release.apk
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_MEDIA_AUDIO
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_EXTERNAL_STORAGE
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.ACCESS_FINE_LOCATION
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.ACCESS_COARSE_LOCATION
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.POST_NOTIFICATIONS
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.RECORD_AUDIO
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_MEDIA_VIDEO
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_MEDIA_IMAGES
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.ACCESS_MEDIA_LOCATION
```

**Para subir a Play Console:**
```powershell
.\gradlew bundleRelease
# AAB en: app\build\outputs\bundle\release\app-release.aab
```

**Comandos DHU:**
```powershell
adb -s dfaf6ffa forward tcp:5277 tcp:5277
Start-Process "C:\Users\jorso\AppData\Local\Android\Sdk\extras\google\auto\desktop-head-unit.exe"
# En móvil: Android Auto → menú → Iniciar servidor de la unidad principal
```

---

## ARQUITECTURA

**Punto de entrada:** `HubActivity` con `CAR_LAUNCHER`
**NO usa CarAppService** (se intentó añadir pero no funcionó)

**Manifest crítico:**
- `android:appCategory="game"` — permite saltarse algunas restricciones AA
- `CAR_LAUNCHER` en HubActivity
- `distractionOptimized="true"` en application y cada activity
- `usesCleartextTraffic="true"` para streams HTTP
- `android:targetSdk=36`, `android:minSdk=29`
- `SYSTEM_ALERT_WINDOW`, `DISABLE_KEYGUARD`, `READ_PHONE_STATE` añadidos
- **NO incluir** `com.google.android.gms.car.application` meta-data (Play Console lo rechaza)

---

## MÓDULOS IMPLEMENTADOS

| Módulo | Archivo | Estado |
|--------|---------|--------|
| Hub principal | `HubActivity.kt` | ✅ Con reloj, fecha y temperatura (IP geolocation fallback) |
| Radio | `RadioCarActivity.kt` | ✅ 3 emisoras: Kiss FM, Hit FM, COPE |
| Música | `MusicCarActivity.kt` | ✅ Layout horizontal lista+player |
| IPTV | `IptvCarActivity.kt` | ✅ Con grupos grid, listas persistentes, menú aspect ratio |
| YouTube | `YoutubeActivity.kt` | ✅ WebView fullscreen sin header propio, adblock |
| Navegador | `BrowserActivity.kt` | ✅ DuckDuckGo, adblock |
| GPS | `GpsCarActivity.kt` | ✅ Lanza Maps/Waze directamente |
| Mirror/Duplicar | `MirrorCarActivity.kt` | ⚠️ No funciona en Android 16 sin root |
| Video | `VideoCarActivity.kt` | ✅ Solo carpeta `/storage/emulated/0/Movies` |
| Consumos | `FuelActivity.kt` | ✅ Historial repostajes con persistencia SharedPreferences |
| Servicio música | `MusicPlaybackService.kt` | ✅ MediaBrowserService + AudioFocus |

**Java (NO TOCAR):** `HomeActivity.java`, `IptvActivity.java`, `MusicActivity.java`, `RadioActivity.java`, `MirrorActivity2.java`, `SettingsActivity.java`

---

## PROBLEMA CRÍTICO: APP NO FUNCIONA EN MOVIMIENTO

### Situación
Android Auto bloquea la app cuando el coche está en movimiento con el mensaje:
**"No disponible mientras conduces"**

### Lo que se intentó (TODO FALLIDO):
1. `android:appCategory="game"` — NO suficiente
2. `distractionOptimized="true"` en todas las activities — NO suficiente
3. Comandos ADB para desactivar detección de conducción — BLOQUEADOS en Android 16
4. `adb shell appops set com.jorso.carapp android:car_app_enabled allow` — Error: operación desconocida
5. `adb shell cmd package set-installer com.jorso.carapp com.android.vending` — Error: SecurityException
6. Añadir CarAppService mínimo con EmptyScreen — NO funcionó
7. Añadir permisos: `SYSTEM_ALERT_WINDOW`, `DISABLE_KEYGUARD`, `FOREGROUND_SERVICE_DATA_SYNC`, `ACCESS_WIFI_STATE` — NO suficiente
8. Subir a Play Console prueba interna — NO suficiente
9. Subir a Play Console prueba abierta — REQUIERE producción primero

### Por qué Fermata SÍ funciona:
- App `me.aap.carplayvideo.auto` (Fermata) está en **Play Store público** (acceso anticipado)
- Usa `AccessibilityEventDispatcherService` — servicio de accesibilidad especial
- Usa `LauncherActivity` como punto de entrada, no HubActivity con CAR_LAUNCHER
- En Android 16 necesita root (LSPosed) o AAWireless para funcionar en movimiento
- El desarrollador de Fermata (AndreyPavlenko) usa hooks de Xposed imposibles sin root

### Análisis APK Fermata (`me.aap.carplayvideo.auto`):
```
Permisos granted=true:
- android.permission.MODIFY_AUDIO_SETTINGS
- android.permission.FOREGROUND_SERVICE
- android.permission.RECEIVE_BOOT_COMPLETED
- android.permission.INTERNET
- com.android.vending.CHECK_LICENSE
- android.permission.USE_FULL_SCREEN_INTENT
- android.permission.FOREGROUND_SERVICE_DATA_SYNC
- android.permission.ACCESS_NETWORK_STATE
- android.permission.DISABLE_KEYGUARD
- android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION
- android.permission.REQUEST_DELETE_PACKAGES
- android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
- android.permission.ACCESS_WIFI_STATE
- android.permission.WAKE_LOCK
- android.permission.POST_NOTIFICATIONS
- android.permission.READ_MEDIA_VISUAL_USER_SELECTED
- android.permission.READ_MEDIA_AUDIO
- android.permission.READ_MEDIA_VIDEO
- android.permission.ACCESS_MEDIA_LOCATION
```

### Solución definitiva pendiente:
**Opción A:** Publicar en Play Store producción (requiere 12 testers en prueba cerrada 14 días)
**Opción B:** AAWireless (~35€) — garantizado al 100%
**Opción C:** Segundo móvil con Android ≤13

---

## EMISORAS DE RADIO FUNCIONANDO

Solo estas 3 emisoras con servidores fiables:
```kotlin
Station("Kiss FM", "https://kissfm.kissfmradio.cires21.com/kissfm.mp3", "Varias FM"),
Station("Hit FM", "https://hitfm.kissfmradio.cires21.com/hitfm.mp3", "Varias FM"),
Station("COPE", "https://flucast09-h-cloud.flumotion.com/cope/net1.aac", "Varias FM")
```

**Servidor streamtheworld.com NO fiable** — redirige múltiples emisoras al mismo stream según región.
**Servidor kissfmradio.cires21.com SÍ fiable** — Kiss FM y Hit FM siempre funcionan.
**RNE (rtvelivestream.akamaized.net)** — No funciona en el dispositivo actual.

---

## IPTV

- Lista por defecto: `https://www.tdtchannels.com/lists/tv.m3u8`
- Arquitectura: Home (listas) → Grid grupos → Player con lista canales
- Persistencia en SharedPreferences `"iptv_prefs"` → `"playlists"` (JSON)
- Soporta URL y archivo local M3U/M3U8
- Menú de aspect ratio al pulsar ⛶: Pantalla completa, Ajustado, 16:9, 4:3, Zoom
- Pantalla completa oculta panel izquierdo, pulsar video vuelve al modo normal

---

## HUB PRINCIPAL

- Header horizontal: título izquierda | reloj+fecha centro | temperatura derecha
- Temperatura usa GPS primero, fallback IP geolocation (ipapi.co) + Open-Meteo API
- Muestra temperatura y ciudad: `"22.5°C  Vila-real"`
- Reloj actualiza cada 1 segundo
- Grid 5 columnas con iconos de módulos

---

## VIDEO

- Solo busca en `/storage/emulated/0/Movies`
- Formatos: mp4, mkv, avi, mov, m4v, 3gp, webm
- Layout horizontal: lista izquierda + reproductor derecha

---

## CONSUMOS (FuelActivity)

- Persistencia: SharedPreferences `"fuel_prefs"` → `"refuels"` (JSON)
- Campos: fecha automática, euros totales, litros, precio/litro (calculado automático)
- Todos los campos opcionales
- Botón ✓ Guardar en el header (siempre visible aunque salga teclado)
- Historial del más reciente al más antiguo
- Totales: litros totales, euros totales, precio medio

---

## YOUTUBE

- WebView fullscreen sin header propio de la app
- Android Auto pone su propio botón ← nativo
- Botón 🏠 flotante esquina inferior derecha para volver al inicio de YouTube
- Adblock por dominio y por URL

---

## DISEÑO GENERAL

- Fondo: `0xFF111111` (negro oscuro)
- Header módulos: `0xFF1A237E` (azul oscuro)
- Padding header: `dp(120)` izquierda para no solapar con botón nativo Android Auto
- Todos los módulos tienen layout horizontal: panel izquierdo lista + panel derecho contenido
- Texto "Música" en strings.xml con encoding UTF-8 correcto

---

## PERMISOS EN MANIFEST

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.ACCESS_MEDIA_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
<uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES"/>
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED"/>
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT"/>
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
```

---

## PENDIENTE PARA FUTURAS SESIONES

1. **App en movimiento** — conseguir 12 testers para prueba cerrada → producción Play Store
2. **WakeLock** — pantalla apagada del móvil bloquea pantalla del coche
3. **Mirror** — no funciona en Android 16 sin root
4. **Módulo Ajustes** — vacío, pendiente: tamaño letra, colores, resolución
5. **Radio** — añadir más emisoras con URLs fiables
6. **IPTV** — favoritos, búsqueda de canales
7. **Música** — barra progreso, artwork álbum
8. **YouTube** — mejorar adblock
9. **Video** — formato aplastado en coche real (pendiente ver foto y ajustar)

---

## NOTAS TÉCNICAS IMPORTANTES

### Encoding
- `strings.xml` debe guardarse en UTF-8 sin BOM
- Los archivos Java dan error `illegal character: '\ufeff'` si se guardan con BOM desde PowerShell
- Usar `[System.IO.File]::WriteAllBytes()` para eliminar BOM si ocurre

### PowerShell
- Los regex multilínea fallan frecuentemente
- Mejor reescribir archivos completos con `@'...'@` que hacer múltiples replace
- El error `DexMerge: archivo en uso` se soluciona cerrando el DHU y ejecutando `.\gradlew clean`
- El error `INSTALL_FAILED_USER_RESTRICTED` se soluciona con `adb install -r --bypass-low-target-sdk-block`

### Android Studio
- Para ediciones puntuales es más fiable editar directamente en Android Studio que con PowerShell
- Los warnings de `onBackPressed deprecated` se suprimen con `@Suppress("OVERRIDE_DEPRECATION")`

### Play Console
- versionCode debe incrementarse en cada subida (último usado: 23)
- No incluir `com.google.android.gms.car.application` en el Manifest
- No incluir `ACCESS_SURFACE` ni `MAP_TEMPLATES` (rechazados por Play Console)
