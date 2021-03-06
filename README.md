# Чепца

Небольшая утилитка для просмотра информации о версии ЦПИ, скачавания отдельных бандлов или целиком архива.

## Установка
В каталоге https://github.com/rsugio/cheptsa/tree/main/releases выложены три артефакта:
1. cheptsa-0.0.1.groovy - скрипт для прописывания в потоке ЦПИ
2. cheptsa-0.0.1.jar - скомпилированный и готовый к использованию сервлет просмотра
3. viewer-0.0.1.zip - поток ЦПИ включающий в себя скрипт груви и сервлет просмотра (п.п.1 и 2), 
 сконфигурированный для вызова с ролью `ESBMessaging.send`

Вы можете импортировать в ЦПИ п.3, задеплоить его и вызвать - это самое простое.
Если есть опасения по импорту, можете скачать данный репозиторий и собрать в гредле 
самостоятельно, при этом также можете сами создать поток.

### Важное в потоке
Необходимо разрешить как минимум заголовок `CamelHttpServletRequest` в параметрах потока, 
этот параметр используется для доступа к сырому запросу. При желании (или если поломает САП)
можно переписать на заголовки CamelHttpUri и прочие, но для простоты локального тестирования
я решил ориентироваться на сервлетную модель а не кэмеловскую.

#### Конфигурация
Единственный конфигурируемый параметр - ендпоинт привязки сервлета, это по умолчанию `/viewer`
что означает в рантайме [https://IFLMAP/http/viewer](IFLMAP/http/viewer). Вы можете поменять значение
на любое удобное вам, я нигде в сервлете не хардкодил.

## Использование
Смотрим в задеплоеный поток и открываем его ендпоинт в браузере, далее всё очевидно.

### Скачивание образа
Скачать можно всё, только АПИ, только адаптеры или только стандартный контент (без потоков, скриптов, ВМов, ММАПов).

### Успешно протестировано
* Neo - 5.30.7 16янв2022
* CF - 6.20.12 17янв2022

# Вопросы, предложения?
iliya.kuznetsov@gmail.com или https://t.me/sapintegration

---
Примечание: Чепца - река на севере Удмуртии
 