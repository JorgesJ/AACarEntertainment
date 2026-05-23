# Script de instalacion AACarEntertainment
Write-Host "Compilando..." -ForegroundColor Cyan
.\gradlew assembleRelease

Write-Host "Desinstalando version anterior..." -ForegroundColor Yellow
adb -s dfaf6ffa uninstall com.jorso.carapp

Write-Host "Instalando APK..." -ForegroundColor Yellow
adb -s dfaf6ffa install app\build\outputs\apk\release\app-release.apk

Write-Host "Concediendo permisos..." -ForegroundColor Yellow
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_MEDIA_AUDIO
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_EXTERNAL_STORAGE
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.ACCESS_FINE_LOCATION
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.ACCESS_COARSE_LOCATION
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.POST_NOTIFICATIONS
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.RECORD_AUDIO
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_MEDIA_VIDEO
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.READ_MEDIA_IMAGES
adb -s dfaf6ffa shell pm grant com.jorso.carapp android.permission.ACCESS_MEDIA_LOCATION

Write-Host "Listo! APK instalado con todos los permisos." -ForegroundColor Green

