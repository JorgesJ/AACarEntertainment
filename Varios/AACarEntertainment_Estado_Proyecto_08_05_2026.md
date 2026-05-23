# AACarEntertainment — Estado del Proyecto

## ⚠️ REGLAS CRÍTICAS PARA EL NUEVO CHAT
- **NO refactorizar** ningún archivo existente sin motivo explícito
- **NO cambiar** la arquitectura HubActivity + CAR_LAUNCHER
- **NO tocar** los archivos Java: `HomeActivity.java`, `IptvActivity.java`, `MusicActivity.java`, `RadioActivity.java`, `MirrorActivity2.java`, `SettingsActivity.java`
- **NO eliminar** módulos existentes sin confirmación
- Hacer **backup** antes de cualquier cambio importante
- Los regex de PowerShell fallan frecuentemente — preferir edición directa en Android Studio o reescritura completa del archivo
- **NO buscar en Drive** a menos que el usuario lo pida explícitamente

---

## DATOS DEL PROYECTO

**Package:** `com.jorso.carapp`
**Ruta:** `C:\Users\jorso\Documents\AACarEntertainment\`
**Keystore:** `aacarentertainment.jks` | alias: `aacarentertainment` | pass: `aacar2026`
**ADB device principal:** `dfaf6ffa` (Xiaomi POCO X6 5G, Android 16, HyperOS/MIUI)
**ADB device viejo:** `5d7eeb19` (Android 12 — descartado, no muestra la app en Android Auto)
**Play Console:** Prueba interna activa | tester: jorsorju@gmail.com
**versionCode actual:** 24 (subido a Play Console prueba interna)

---

## SCRIPT DE INSTALACIÓN

`instalar.ps1` en raíz del proyecto (para el POCO X6):
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

**Compilar solo APK (para instalar manualmente):**
```powershell
.\gradlew assembleRelease
# APK en: app\build\outputs\apk\release\app-release.apk
```

**Si el build no actualiza el archivo:**
```powershell
.\gradlew clean
.\gradlew bundleRelease
```

**Cambiar versionCode (PowerShell):**
```powershell
(Get-Content "app\build.gradle.kts" -Raw) -replace 'versionCode = 23', 'versionCode = 24' | Set-Content "app\build.gradle.kts" -Encoding UTF8
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
| Hub principal | `HubActivity.kt` | ✅ Con reloj, fecha, temperatura y permisos en runtime |
| Radio | `RadioCarActivity.kt` | ✅ 7 emisoras con logos, agrupadas por categoría |
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
4. Añadir CarAppService mínimo con EmptyScreen — NO funcionó
5. Subir a Play Console prueba interna — NO suficiente
6. Subir a Play Console producción con testers — **CONFIRMADO QUE NO FUNCIONA** (app de terceros comprada con 200+ testers también dejó de funcionar hace ~2 semanas)
7. Móvil viejo Android 12 — **DESCARTADO**, no muestra la app en Android Auto
8. Android Auto versión 16.6.661454 (móvil viejo) vs 16.6.661444 (POCO X6) — no es la causa

### Análisis situación actual:
- Google ha endurecido restricciones en Android Auto recientemente (~últimas 2 semanas de abril 2025)
- Apps compradas con producción Play Store y 200+ testers también han dejado de funcionar
- No hay parche ADB ni truco conocido que funcione en Android 16 sin root

### Solución definitiva pendiente:
**AAWireless (~35€)** — única opción confirmada que garantiza funcionamiento en marcha

### Pendiente analizar:
- APK de app comprada que SÍ funciona en marcha — pendiente analizar en nuevo chat para ver qué diferencia tiene con la nuestra

---

## RADIO

### Emisoras funcionando (7)
```kotlin
// Generalistas
Station("COPE",    "https://flucast09-h-cloud.flumotion.com/cope/net1.aac", "Generalistas", R.drawable.radio_cope),
Station("esRadio", "http://livestreaming.esradio.fm/stream64.mp3",          "Generalistas", R.drawable.radio_esradio),

// Música
Station("Los 40",      "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_SC",       "Música", R.drawable.radio_los40),
Station("Cadena Dial", "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENADIAL.mp3", "Música", R.drawable.radio_cadenadial),
Station("Cadena 100",  "https://cadena100-streamers-mp3.flumotion.com/cope/cadena100.mp3",                "Música", R.drawable.radio_cadena100),
Station("KISS FM",     "https://kissfm.kissfmradio.cires21.com/kissfm.mp3",                               "Música", R.drawable.radio_kissfm),
Station("Hit FM",      "https://hitfm.kissfmradio.cires21.com/hitfm.mp3",                                 "Música", R.drawable.radio_hitfm),
```

### Logos de emisoras
- Carpeta: `app/src/main/res/drawable/`
- Archivos: `radio_cope.png`, `radio_esradio.png`, `radio_los40.png`, `radio_cadenadial.png`, `radio_cadena100.png`, `radio_kissfm.png`, `radio_hitfm.png`
- El logo se muestra en el panel derecho (160x160dp) al seleccionar emisora

### Emisoras descartadas (sin stream público estable):
- RNE (todas) — servidor rtveradio.cires21.com no conecta
- Cadena SER — bucle sin audio
- Onda Cero — no conecta
- Europa FM — no conecta
- Rock FM — no conecta
- MegaStarFM — no conecta
- Radio Marca — no conecta
- Vaughan Radio — no conecta

---

## HUB PRINCIPAL

- Header horizontal: título izquierda | reloj+fecha centro | temperatura derecha
- Temperatura usa GPS primero, fallback IP geolocation (ipapi.co) + Open-Meteo API
- Muestra temperatura y ciudad: `"22.5°C  Vila-real"`
- Reloj actualiza cada 1 segundo
- Grid 5 columnas con iconos de módulos
- **Permisos en runtime**: se piden automáticamente al primer arranque, adaptados por versión Android (13+ usa READ_MEDIA_*, 12 e inferior usa READ_EXTERNAL_STORAGE)

---

## IPTV

- Lista por defecto: `https://www.tdtchannels.com/lists/tv.m3u8`
- Arquitectura: Home (listas) → Grid grupos → Player con lista canales
- Persistencia en SharedPreferences `"iptv_prefs"` → `"playlists"` (JSON)
- Soporta URL y archivo local M3U/M3U8
- Menú de aspect ratio al pulsar ⛶: Pantalla completa, Ajustado, 16:9, 4:3, Zoom
- Pantalla completa oculta panel izquierdo, pulsar video vuelve al modo normal

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

1. **App en movimiento** — analizar APK de app comprada que sí funciona en marcha para ver qué diferencia tiene
2. **AAWireless** — considerar compra (~35€) como solución garantizada
3. **WakeLock** — pantalla apagada del móvil bloquea pantalla del coche
4. **Mirror** — no funciona en Android 16 sin root
5. **Módulo Ajustes** — vacío, pendiente: tamaño letra, colores, resolución
6. **Radio** — buscar URLs fiables para SER, Onda Cero, Europa FM, Rock FM, MegaStarFM
7. **IPTV** — favoritos, búsqueda de canales
8. **Música** — barra progreso, artwork álbum
9. **YouTube** — mejorar adblock
10. **Video** — formato aplastado en coche real (pendiente ver foto y ajustar)

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
- El error `INSTALL_FAILED_USER_RESTRICTED` en móvil sin SIM — no tiene solución por ADB, instalar manualmente el APK

### Android Studio
- Para ediciones puntuales es más fiable editar directamente en Android Studio que con PowerShell
- Los warnings de `onBackPressed deprecated` se suprimen con `@Suppress("OVERRIDE_DEPRECATION")`
- El archivo de build es `app\build.gradle.kts` (no `build.gradle`)

### Play Console
- versionCode debe incrementarse en cada subida (último usado: 24)
- No incluir `com.google.android.gms.car.application` en el Manifest
- No incluir `ACCESS_SURFACE` ni `MAP_TEMPLATES` (rechazados por Play Console)
- Producción con testers NO garantiza funcionamiento en marcha — confirmado

### Google Drive (conector Claude)
- Activar "Herramientas ya cargadas" en el icono + del chat antes de empezar
- Si da "No approval received" es que la sesión OAuth expiró — reconectar en Ajustes → Conectores
