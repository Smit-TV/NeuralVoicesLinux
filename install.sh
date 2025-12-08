#!/usr/bin/env bash

echo "Начинаем установку"
echo "Файлы приложения будут помещены в ~/.local/bin"
mkdir -p ~/.local/bin/neuralVoices
cp app/build/libs/app.jar ~/.local/bin/neuralVoices/
cp bin/neuralvoices ~/.local/bin/
chmod +x ~/.local/bin/neuralvoices
echo "Готово"