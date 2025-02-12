<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Валентинка</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background: linear-gradient(to bottom, #ff4d6d, #ff758f);
            text-align: center;
            color: white;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
        }
        h1 {
            font-size: 26px;
            margin-bottom: 30px;
        }
        .buttons {
            display: flex;
            gap: 15px;
        }
        button {
            font-size: 18px;
            padding: 10px 20px;
            border: none;
            cursor: pointer;
            border-radius: 10px;
            transition: 0.3s;
        }
        #yes {
            background-color: #ff4d6d;
            color: white;
        }
        #yes:hover {
            background-color: #ff1e4d;
        }
        #no {
            background-color: white;
            color: #ff4d6d;
        }
        #no:hover {
            background-color: #ffb3c1;
        }
        .message {
            font-size: 22px;
            font-weight: bold;
            margin-top: 20px;
            opacity: 0;
            transition: opacity 0.5s;
        }
    </style>
</head>
<body>

    <h1>Олечка,ты будешь со мной на день Святого Валентина?</h1>
    
    <div class="buttons">
        <button id="yes">Да</button>
        <button id="no">Нет</button>
    </div>

    <p class="message" id="response"></p>

    <script>
        document.getElementById("yes").addEventListener("click", function() {
            let message = document.getElementById("response");
            message.innerHTML = "люблю тебя,Оль💋💋";
            message.style.opacity = "1";
        });

        document.getElementById("no").addEventListener("click", function() {
            let message = document.getElementById("response");
            message.innerHTML = "пизда😶";
            message.style.opacity = "1";
        });
    </script>

</body>
</html>
