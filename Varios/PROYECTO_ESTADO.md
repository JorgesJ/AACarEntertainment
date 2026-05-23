# AACarEntertainment — Estado del Proyecto
*Generado el 21/03/2026*

---

## 1. RESUMEN DEL PROYECTO

App Android Auto llamada **AACarEntertainment** para mostrar módulos (Música, Radio, YouTube, IPTV, Navegador, GPS, Duplicar, Ajustes) en la pantalla del coche Alfa Romeo con head unit compatible.

**Dispositivo:** Xiaomi POCO X6 5G, Android 16, Android Auto 16.3.660834

---

## 2. DATOS DEL PROYECTO

| Campo | Valor |
|-------|-------|
| Package | `com.jorso.carapp` |
| Namespace | `com.jorso.carapp` |
| Keystore | `C:\Users\jorso\Documents\AACarEntertainment\aacarentertainment.jks` |
| Keystore alias | `aacarentertainment` |
| Store/Key password | `aacar2026` |
| SHA1 | `A2:06:7A:A4:8C:7F:19:3E:AC:98:FC:D7:E4:EB:BE:79:26:71:37:B5` |
| Proyecto | `C:\Users\jorso\Documents\AACarEntertainment\` |
| AGP | 8.5.2 |
| Kotlin | 2.0.21 |
| compileSdk | 35, minSdk 29 |
| Play Console | Prueba interna, tester: jorsorju@gmail.com |
| versionCode actual | 15 |

---

## 3. LO QUE FUNCIONA

### ✅ Play Store / Android Auto
- La app aparece en el launcher de Android Auto (pantalla del coche y DHU)
- El grid de 8 módulos se muestra correctamente en la pantalla del coche
- La app debe instalarse desde Play Store (prueba interna) para aparecer en AA — NO funciona instalada por ADB

### ✅ Car App Library (Templates)
- `CarAppService` + `AACarSession` + `HomeScreen` funcionan correctamente
- `GridTemplate` con 8 módulos se renderiza bien en coche y DHU
- `ListTemplate` funciona — `RadioScreen` muestra la lista de emisoras
- `screenManager.push()` funciona en DHU, **pendiente confirmar en coche**

### ✅ Radio (DHU)
- `RadioScreen.kt` muestra lista de 3 emisoras
- ExoPlayer arranca (estado 3 = READY) pero audio no suena en DHU (normal)
- En coche real: la lista se abre en el móvil en lugar del coche (versión anterior con launchActivity)
- **Pendiente probar versión 15 en coche con cobertura**

### ✅ Módulos móvil (funcionan en el móvil, NO en coche aún)
- MusicActivity — ExoPlayer, selector de carpeta, reproduce MP3
- RadioActivity — 3 emisoras funcionando (Los 40, Cadena SER, Kiss FM)
- YoutubeActivity — WebView YouTube
- BrowserActivity — WebView con barra URL
- IptvActivity — ExoPlayer + M3U con persistencia de URL

---

## 4. LO QUE NO FUNCIONA / PROBLEMAS

### ❌ Activities normales en coche
- `carContext.startActivity()` → SecurityException en coche real
- `carContext.startCarApp()` → abre en móvil, no en pantalla del coche
- `PendingIntent` → mismo resultado
- PROYECCIÓN (`CATEGORY_PROJECTION`) → MIUI marca `isNotProjection=true`, `activity count=0`
- El SDK privado de Google (`CarActivityService`) que usa la app de referencia no está disponible públicamente

### ❌ Música en DHU
- MediaStore no indexa las canciones de `/storage/emulated/0/Music/Musica Remember/Mi Musica/`
- `File.listFiles()` devuelve vacío aunque el directorio existe (permisos Android 13+)
- Permisos `READ_MEDIA_AUDIO` concedidos manualmente por ADB pero MediaStore sigue vacío
- Reinicio del móvil no resolvió el problema
- **Canciones disponibles:** PACHA1989.mp3, PACHA 4 aniversario.mp3, Kike Jaén 39 Aniversario ACTV.mp3, Sesion N O D Kike Jaen.mp3, + archivos .wav

### ❌ Audio en DHU
- ExoPlayer en RadioScreen alcanza STATE_READY pero audio no sale por altavoces del PC/móvil
- Necesita MediaSession para que Android Auto gestione el audio correctamente

---

## 5. ARQUITECTURA ACTUAL

### Sistema elegido: Car App Library (Templates)
Después de probar PROJECTION (fallido por restricciones MIUI/Android 16), se usa Car App Library con templates oficiales de Google.

### Flujo de la app:
```
Play Store → AACarEntertainmentCarService (CarAppService)
                    ↓
              AACarSession (Session)
                    ↓
              HomeScreen (GridTemplate) ← 8 módulos
                    ↓
         screenManager.push(RadioScreen) ← ListTemplate ✅
         screenManager.push(MusicScreen) ← ListTemplate ⚠️ pendiente
         screenManager.push(ComingSoonScreen) ← resto de módulos
```

### Archivos Kotlin (Car App Library):
- `AACarEntertainmentCarService.kt` — extiende CarAppService
- `AACarSession.kt` — extiende Session, devuelve HomeScreen
- `HomeScreen.kt` — GridTemplate con 8 módulos
- `RadioScreen.kt` — ListTemplate con emisoras + ExoPlayer ✅
- `MusicScreen.kt` — ListTemplate + MediaStore ⚠️
- `ComingSoonScreen.kt` — MessageTemplate placeholder

### Archivos Java (Activities móvil — NO usadas en coche):
- `HomeActivity.java`, `MusicActivity.java`, `RadioActivity.java`
- `IptvActivity.java`, `YoutubeActivity.java`, `BrowserActivity.java`
- `MirrorActivity2.java`, `SettingsActivity.java`

---

## 6. MANIFEST ACTUAL (puntos clave)

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>
<uses-permission android:name="android.permission.INTERNET"/>
<!-- ... otros permisos -->

<meta-data android:name="com.google.android.gms.car.application"
    android:resource="@xml/automotive_app_desc"/>
<meta-data android:name="androidx.car.app.minCarApiLevel" android:value="5"/>

<service android:name=".auto.AACarEntertainmentCarService" android:exported="true">
    <intent-filter>
        <action android:name="androidx.car.app.CarAppService"/>
        <category android:name="androidx.car.app.category.POI"/>
    </intent-filter>
</service>
```

### automotive_app_desc.xml:
```xml
<automotiveApp>
    <uses name="projection"/>
    <uses name="template"/>
    <uses name="media"/>
    <uses name="navigation"/>
</automotiveApp>
```

---

## 7. RADIO — EMISORAS FUNCIONANDO

```
Los 40: https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40.mp3
Cadena SER: https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASER.mp3
Kiss FM: https://kissfm.kissfmradio.cires21.com/kissfm.mp3
```

---

## 8. COMANDOS IMPORTANTES

### DHU (Desktop Head Unit):
```powershell
adb -s dfaf6ffa forward tcp:5277 tcp:5277
Start-Process "C:\Users\jorso\AppData\Local\Android\Sdk\extras\google\auto\desktop-head-unit.exe"
```
En móvil: Android Auto → menú → "Iniciar servidor de la unidad principal"

### Compilar debug (pruebas):
```powershell
.\gradlew assembleDebug
adb -s dfaf6ffa uninstall com.jorso.carapp
adb -s dfaf6ffa install app\build\outputs\apk\debug\app-debug.apk
```

### Compilar release (Play Console):
```powershell
(Get-Content "app\build.gradle.kts") -replace 'versionCode = \d+', 'versionCode = 16' | Set-Content "app\build.gradle.kts"
.\gradlew bundleRelease
```
AAB en: `app\build\outputs\bundle\release\app-release.aab`

### Logcat filtrado:
```powershell
adb -s dfaf6ffa logcat 2>&1 | Select-String "MusicScreen|RadioScreen|HomeScreen|CarApp|Exception"
```

---

## 9. PRÓXIMOS PASOS

### Inmediato — MusicScreen:
El problema es que MediaStore no indexa las canciones. Opciones:
1. Usar `MediaScannerConnection.scanFile()` dentro de la app para forzar indexado
2. Usar `ContentResolver` con query por `RELATIVE_PATH` 
3. Usar `StorageManager` y `DocumentFile` para acceso directo

### Pendiente confirmar en coche real:
- ✅ Grid HomeScreen visible en coche
- ⚠️ RadioScreen: ¿aparece la lista en pantalla del coche? (versión 15 pendiente probar)
- ⚠️ Audio Radio: ¿suena por altavoces del coche?

### Módulos por implementar como Screen:
- MusicScreen ⚠️ (problema MediaStore)
- IptvScreen (ListTemplate + ExoPlayer + M3U)
- BrowserScreen (limitado — Google no permite WebView en car)
- YoutubeScreen (limitado — igual que Browser)
- GpsScreen (selector de navegador: Maps/Waze)
- MirrorScreen
- SettingsScreen

---

## 10. REFERENCIA — APP QUE FUNCIONA

La app `com.aacarplayvideo.app` (APK descompilado disponible) usa:
- SDK privado de Google: `com.google.android.apps.auto.sdk.CarActivityService`
- `CarActivity` para renderizar Activities en pantalla del coche
- `CATEGORY_PROJECTION` + `CATEGORY_PROJECTION_OEM`
- **Este SDK no está disponible públicamente** — solo para partners certificados de Google

---

## 11. POLÍTICA DE PRIVACIDAD
URL: https://sites.google.com/view/aacarentertainment/inicio
