# NeuralVoices для Linux
- Этот проект  позволяет использовать нейронные голоса Microsoft на Linux
## Зависимости
1. gradle
2. kotlin
3. java
4. Любой голос Microsoft
### Сборка
- Выполните
```bash
git clone https://github.com/Smit-TV/neuralVoices && cd ./neuralVoices && ./build.sh
```
#### Установка
- После сборки выполните
```bash
./install.sh
```
- После установки можно получить справку по использованию так:
```bash
neuralvoices --help
```
- Не забудьте поместить все голоса в папку  ~/.neuralvoices
##### Помощь
- Если есть предложения, замечания или просто хотите сделать свой вклад то можете создать issue или написать на i@altairait.kz
###### Премечание
- Данный проект использует зависимости от Microsoft Azure CognitiveServices