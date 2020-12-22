# PolyTeX
Web-based LaTeX compiler/interpreter 
https://latex.icst.spbstu.ru/
(login/password: test/test)

# Инструкция по запуску приложения.

1) Установить необходимые программы и компиляторы:
	- Java 11 JDK - комплект разработчика приложений на языке Java (sudo apt install openjdk-11-jdk),
	- Gradle — система автоматической сборки (sudo apt install gradle),
	- TeX Live full - дистрибутив LaTeX (sudo apt install texlive-full),
	- biber - сборщик библиографии (sudo apt install biber).

2) Компиляция проекта:
	- 2.1) Перейти в корень репозитория с исходным кодом;
	- 2.2) Выполнить команду gradlew build;
	- 2.3) Нужный в дальнейшем исполняемый .jar файл будет находиться в подкаталоге build\libs.

3) Запуск сервера приложения:
	- 3.1) В каталоге с .jar файлом создать папки: config, database, projects и templates;
	- 3.2) В папку config поместить application.yaml и application.properties из одноимённой папки в корне репозитория проекта.
	- 3.3) Запустить программу командой java -jar ИМЯ_ИСПОЛНЯЕМОГО_ФАЙЛА или для работы в фоновом режиме сделать обёрточный сервис для программы (http://jcgonzalez.com/ubuntu-16-java-service-wrapper-example).
	
Примечания: 
1) application.yml содержит некоторые закомментированные параметры файла, которые необходимо задать самостоятельно,
2) для сброса всех данных приложения можно удалить содержимое папок database, projects и templates,
3) файлы проекта находятся в подпапке projects, имеющей имя соответствующее id проекта в базе данных,
4) для добавления пользователя-администратора в БД при запуске приложения необходимо при сборке проекта добавить файл data.sql по пути src\main\resources, который будет содержать строку вида:
    - merge into USER(id, email, password, role, banned)
    - select '6fe763e29b8a11eabb370242ac130002', 'admin', 'Хэш пароля, полученный функцией BCryptPasswordEncoder(4).encode("пароль")', 'ROLE_ADMIN', 'FALSE' from DUAL;

### Special thanks to: [Donutellko](https://github.com/Donutellko)

# TODO:

## Backend:

1. ???

## Frontend:

1. Tab with latex error.
2. Add line numbers.
3. LaTeX to Richtext conversion.
4. Add WYSIWYG formulae, pictures and tables insertion.
5. Make it look more smooth.
