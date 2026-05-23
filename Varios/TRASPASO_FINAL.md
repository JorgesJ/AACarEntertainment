# AACarEntertainment — Documento de Traspaso DEFINITIVO v3
*25/04/2026 — LEER TODO ANTES DE EMPEZAR*

---

## ⚠️ REGLAS CRÍTICAS

1. **NO refactorices** código sin confirmación del usuario
2. **Backup SIEMPRE** antes de cualquier cambio
3. **Prueba en DHU** antes de compilar release
4. **Un cambio a la vez** — compilar, probar, luego el siguiente
5. **Usar APK release** (`assembleRelease`) no debug para pruebas
6. **NO subir a Play Console** con permisos ACCESS_SURFACE/NAVIGATION_TEMPLATES
7. El usuario tiene un hijo con autismo — IPTV/vídeo es muy importante
8. **NO recrear archivos que ya existen** — leer la sección de archivos primero

---

## DATOS DEL PROYECTO

| Campo | Valor |
|-------|-------|
| Package | `com.jorso.carapp` |
| Ruta | `C:\Users\jorso\Documents\AACarEntertainment\` |
| Keystore | `aacarentertainment.jks` — alias: `aacarentertainment` — pass: `aacar2026` |
| ADB device | `dfaf6ffa` (Xiaomi POCO X6 5G, Android 16, HyperOS) |
| versionCode | **17** — próxima será **20** |
| Play Console | Prueba interna — tester: jorsorju@gmail.com |
| Car App Library | 1.4.0 — Car API Level: 7 |

---

## COMANDOS ESENCIALES

### Compilar e instalar (SIEMPRE release):
```powershell
.\gradlew assembleRelease
adb -s dfaf6ffa uninstall com.jorso.carapp
adb -s dfaf6ffa install app\build\outputs\apk\release\app-release.apk
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_MEDIA_AUDIO
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_EXTERNAL_STORAGE
```

### Abrir DHU:
```powershell
adb -s dfaf6ffa forward tcp:5277 tcp:5277
Start-Process "C:\Users\jorso\AppData\Local\Android\Sdk\extras\google\auto\desktop-head-unit.exe"
```
En móvil: **Android Auto → menú → Iniciar servidor de la unidad principal**

### Backup:
```powershell
$fecha = Get-Date -Format "yyyyMMdd_HHmm"
Copy-Item -Path "C:\Users\jorso\Documents\AACarEntertainment" -Destination "C:\Users\jorso\Documents\AACarEntertainment_backup_$fecha" -Recurse
Write-Host "Backup creado"
```

---

## ESTADO ACTUAL — FUNCIONA CON APK RELEASE

### ✅ Funciona:
- **Radio** — 3 emisoras, audio OK (AudioFocus implementado en RadioScreen.kt)
- **Música** — canciones locales, reproducción, controles (PaneTemplate)
- **IPTV** — lista canales TDT, vídeo + audio (SurfaceCallback + NavigationTemplate)

### ❌ Pendiente:
- YouTube, Navegador, GPS, Ajustes — stubs vacíos
- Duplicar (Mirror) — imposible con MIUI Android 16 sin root

---

## ARCHIVOS ACTUALES EN auto/

### ✅ YA CREADOS — NO recrear:

**HubActivity.kt** — Grid principal con diseño nuevo:
- Fondo negro `#111111`
- Header "AACarEntertainment" + subtítulo gris
- Grid 5 columnas con círculos de colores
- Música=azul, Radio=verde, IPTV=rojo, YouTube=rojo, Navegador=morado, GPS=cyan, Mirror=amarillo, Ajustes=gris
- Efecto ripple al pulsar
- Usa iconos existentes `ic_module_*.xml`
- Llama a: YoutubeActivity, BrowserActivity, RadioCarActivity, MusicCarActivity, IptvCarActivity, GpsCarActivity

**Stubs vacíos (solo hacen finish()) — pendiente implementar:**
- `YoutubeActivity.kt`
- `BrowserActivity.kt`
- `RadioCarActivity.kt`
- `MusicCarActivity.kt`
- `IptvCarActivity.kt`
- `GpsCarActivity.kt`

**Funcionales — mantener:**
- `MusicPlaybackService.kt` — MediaBrowserService + AudioFocus
- `RadioScreen.kt` — funciona con AudioFocus
- `MusicScreen.kt` — funciona con PaneTemplate
- `IptvPlayerScreen.kt` — funciona con SurfaceCallback

**Obsoletos — eliminar cuando nueva arquitectura funcione:**
- `AACarEntertainmentCarService.kt`
- `AACarSession.kt`
- `HomeScreen.kt`
- `ComingSoonScreen.kt`
- `GpsScreen.kt`
- `IptvScreen.kt`
- `YoutubeScreen.kt` ← eliminar YA, es un intento fallido

### Java en auto/ — NO TOCAR:
- `IptvActivity.java`, `MusicActivity.java`, `RadioActivity.java`
- `MirrorActivity2.java`, `SettingsActivity.java`, `HomeActivity.java`

### Java en ui/ — NO TOCAR:
- `MainActivity.java` — pide permisos READ_MEDIA_AUDIO

---

## PROBLEMA ACTUAL Y SOLUCIÓN

### Problema:
`HubActivity` está creada con el diseño nuevo pero **Android Auto la ignora** porque `CarAppService` sigue en el Manifest y tiene prioridad.

### Solución — PASO 1 (lo primero que hay que hacer):
Eliminar CarAppService del Manifest:
```powershell
(Get-Content "app\src\main\AndroidManifest.xml" -Raw) -replace '(?s)<service android:name="\.auto\.AACarEntertainmentCarService".*?</service>', '' | Set-Content "app\src\main\AndroidManifest.xml"
```

Cambiar automotive_app_desc.xml:
```powershell
'<?xml version="1.0" encoding="utf-8"?><automotiveApp><uses name="media"/></automotiveApp>' | Set-Content "app\src\main\res\xml\automotive_app_desc.xml"
```

Verificar:
```powershell
Select-String "CarAppService" "app\src\main\AndroidManifest.xml"
Get-Content "app\src\main\res\xml\automotive_app_desc.xml"
```

El primer comando no debe devolver nada. Luego compilar release e instalar.

---

## HOJA DE RUTA COMPLETA

### PASO 1 — Eliminar CarAppService ← EMPEZAR AQUÍ
Ver comandos arriba. Verificar que HubActivity aparece en DHU con el grid circular.

### PASO 2 — YoutubeActivity (WebView)
Implementar con `https://m.youtube.com`, User Agent tablet, JS adblock.

### PASO 3 — BrowserActivity (WebView)
WebView con barra URL editable.

### PASO 4 — RadioCarActivity
Migrar lógica de RadioScreen.kt con ExoPlayer + AudioFocus.

### PASO 5 — MusicCarActivity
Migrar MusicPlaybackService.kt con portada, barra progreso, controles.

### PASO 6 — IptvCarActivity
Migrar IptvPlayerScreen.kt con SurfaceCallback para vídeo.

### PASO 7 — GpsCarActivity
Intent normal para Maps/Waze (desde Activity SÍ funciona, desde CarContext NO).

### PASO 8 — Limpiar obsoletos
Eliminar archivos de Car App Library que ya no se usan.

### PASO 9 — Distribuir APK release directamente
Como hace el autor de la app de referencia — APK firmado compartido directamente.

---

## APP DE REFERENCIA ANALIZADA

- Package: `com.aacarplayvideo.fullscreen`
- Decompilada disponible — úsala como referencia
- NO usa CarAppService — solo HubActivity con CAR_LAUNCHER
- automotive_app_desc.xml solo tiene `<uses name="media"/>`
- Se distribuye por APK directo (Telegram), no Play Store
- Tiene sistema de licencias propio en `aacarplayvideo.com/api/`

---

## CONTENIDO

### Radio:
```
Los 40: https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40.mp3
Cadena SER: https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASER.mp3
Kiss FM: https://kissfm.kissfmradio.cires21.com/kissfm.mp3
```

### IPTV: `https://www.tdtchannels.com/lists/tv.m3u8`
### Música: `/storage/emulated/0/Music/Musica Remember/Mi Musica/`
### Privacidad: `https://sites.google.com/view/aacarentertainment/inicio`

---

## CONTEXTO

- Distribución: APK directo — NO Play Store bundle (rechaza permisos de superficie)
- Monetización: cobrar fuera de Google (Bizum/PayPal), añadir testers a Play Console
- Objetivo: pagar terapias del hijo con autismo
- MIUI Android 16 bloquea mirror — imposible sin root
- Backups en `C:\Users\jorso\Documents\` formato `AACarEntertainment_backup_YYYYMMDD_HHMM`
