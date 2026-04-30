@echo off
echo 🎻 Building Horrible Violin...
cd src
javac com/violin/*.java
if %errorlevel% equ 0 (
    echo ✅ Success! Starting the horror...
    java com.violin.Main
) else (
    echo ❌ Something broke lmao
    pause
)