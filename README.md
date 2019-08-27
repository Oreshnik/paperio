Расскажу немного про свое решение в соревновании AiCups 4 Paperio.

Так как в силу обстоятельств писать бота я начала спустя почти две недели после старта бета-теста, то изначально я решила писать максимально просто не вдаваясь в мелкие детали реализации игрового мира. Игровая механика была вроде относительно простой, так что я начала писать код, даже не заглядывая в код локал-раннера. Микроклетки поля мне показались излишним усложнением, так что все игровые координаты я сразу же при чтении стала простым округлением переводить в координаты больших клеток.

<h1>Поиск</h1>
Итак, от бота требуется, чтобы он обводил территорию наиболее эффективным образом, не делая лишних путей, захватывая побольше вражеских клеток и при этом берег свой хвост от пересечения соперниками. Какое-либо аналитическое решение мне придумать было сложно, а когда логика отказывает, на помощь всегда приходит перебор.

В качестве метода поиска я сначала планировала использовать MCTS, но начала с простого случайного поиска. Вообще мне нравится случайный поиск тем, что он очень прост в отладке - если кажется, что бот ведет себя неправильно, можно просто взять решение, которое бот посчитал лучшим, и увидеть, какая ошибка привела в переоценке неправильного поведения. Это выгодно отличает его от статистических методов вроде MCTS или Monte Carlo, которые условно говорят, что после 10 тысяч симуляций этот ход статистически выбран лучшим, и, чтобы понять, почему, придется сильно постараться. С другой стороны случайный поиск обладает рядом неприятных недостатков. Например, он может многократно рассматривать одни и те же решения; после определенного порога увеличение времени работы практически не приводит к улучшению результата; лучшее найденное решение может дать ход статистически малоперспективный. Тем не менее, случайный поиск - это неплохое стартовое решение, которое относительно легко можно переписать на другой поисковый алгоритм после отладки оценочной функции. В моем случае <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L20">случайный поиск</a> так и не был ничем заменен.

Путь генерировался из всех <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L671">разрешенных ходов</a> до тех пор пока не возвращался на свою базу или же не превышал максимальной длины 16. При этом движение в том же направлении выбиралось с вероятностью 0.5, в противном случае выбиралось случайное изо всех доступных направлений. 

<a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L662">Откат</a> бота после оценки пути в начальное состояние получился очень простым - надо было просто удалить из хвоста точки, полученные при генерации пути, и вернуть ему начальное положение и направление.

<h1>Оценка</h1>
Для оценки успешного пути надо было посчитать <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L577">заливку</a>. Тут все сделано было просто: сначала от краев <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L639">отмечаем</a> всю чужую/нейтральную территорию, затем от точек хвоста <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L595">считаем</a> все оставшееся, что не относится к старой заливке. Другие участники писали, что как-то оптимизировали этот алгоритм, но у меня он хотя поначалу и занимал 80% времени, но в итоге проблем не доставлял. При этом, так как вся генерация после заливки заканчивалась, не было необходимости изменять состояние бота по заливке и потом откатывать его.

Для того, чтобы форма заливки была по-возможности оптимальной, для оценки была выбрана формула: <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L392">заливка^2/длину пути</a>. Это избавляло от лишних изгибов и поворотов, но при этом с каждым ходом боту хотелось захватить все больше территории и все меньше хотелось возвращаться на свою базу. Поэтому сначала я добавила штраф, увеличивающийся после определенной длины пути, но потом, решив, что естественной опасности будет достаточно для возвращения на базу и убрала его. 

Еще мне хотелось, чтобы бот не просто захватывал побольше территории, но и захватывал ее поближе к территории противника. Поэтому к оценке я добавила влияние <a href="https://github.com/Oreshnik/paperio/blob/master/src/model/World.java#L127">потенциального поля</a> на линию. Это привело к нежелательному эффекту - бот стал петлять, стараясь заработать побольше очков. Пришлось оставить только одно, максимальное значение поля на линии. Но толком я это так и не протестировала и не знаю, полезно ли оно было в итоге.

Дальше к оценке добавлялся <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L413">штраф</a> в случае, если длина пути больше чем расстояние до соперников. Тут стала сказываться неточность с координатами и иногда соперники таки догоняли мой хвост. Но не беда, добавляем +1 или +2 к разнице и все работает.
Другие участники поступали более умно, не оценивая те пути, которые могут привести к пересечению хвоста, но мне как-то в голову это не пришло.

Также добавлялся штраф за замедление, но работал он у меня плохо, так как из-за квадрата очков бот был слишком жадным и ради большого захвата часто штрафом за замедление пренебрегал.

Для случая, если путь не завершался на базе, он достраивался к <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L267">базе поиском пути</a>. Но, судя по тому, что в логе такие достроенные пути мне не попадались, это было довольно бесполезной доработкой.
Для случая, когда путь не вышел с базы, он <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L387">оценивался</a> по влиянию потенциального поля для первого хода.
<h1>Борьба с окружением</h1>
В какой-то момент самой большой проблемой бота стало то, что его или планируемую точку окружал соперник. Чтобы предсказать такое окружение, для каждого соперника, не находящегося на базе, я стала считать кратчайший путь до базы, соответствующую ему заливку, и проверять что и когда попадает в окружение. Работало это плохо, так как ближайшей точкой чаще всего оказывались точки из которых противник только что вышел, а вовсе не те, куда он планировал прийти. В итоге мой бот замечал окружение когда уже поздно было что-то делать.

Пришлось делать более <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L103">расширенную версию</a> обнаружения окружения. Для этого я искала пути до всех точек базы соперника, которые были от него на расстоянии меньшим или равным 10. И для каждого из этих путей считала заливку, попадание в область каждой из которых <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L455">штрафовалось</a> как окружение. Надо сказать, что это решение, тоже далеко от идеального, так как кратчайших путей до точки может быть множество.
<h1>Повышение точности</h1>
На этом идеи у меня кончилось и я пошла смотреть на игры топов. Очень быстро я заметила, что в сравнении с ними мой бот ведет себя слишком осторожно. Я попробовала изменить запас по опасности с 2 до 1 и тут же была наказана противниками. Похоже, что пришло время погрузиться в микроклетки с микротиками. 
Скоро стало понятно, что проблема не просто в округлении микроклеток. Для корректного расчета ходов мне понадобилось <a href="https://github.com/Oreshnik/paperio/blob/master/src/model/World.java#L49">считать</a> клеткой соперника клетку, куда он только прибудет, плюс дополнительное время, которое ему для этого понадобится. Код получился довольно сложным, но на удивление заработал с первого раза. Дополнительно я сделала <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L133">предрасчет расстояний</a> противников до всех клеток с учетом их хвостов и направление.
Теперь я смогла избавиться от <a href="https://github.com/Oreshnik/paperio/blob/master/src/model/Constants.java#L8">запаса по опасности</a> и мой бот стал захватывать гораздо больше территории, часто вместо с противниками, что подняло его сильно выше в рейтинге. 

Правда тут возникла проблема с коллизиями. Ведь если для того, чтобы перерезать хвост, нужно полностью зайти на клетку хвоста, то для коллизии достаточно любого пересечения, от которого больше не защищал буфер. <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L512">Борьбой</a> коллизиями я занималась оставшиеся два дня соревнования.
<h1>Оптимизации</h1>
<h2>Логические</h2>
Как я уже писала, проблема случайного поиска в том, что он довольно часто может находить одинаковые пути. А если такой путь мы уже находили, то зачем считать его еще раз? Поэтому я добавила <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L578">сохранение</a> значения заливки для всех сгенерированных путей.

После этого количество обработанных путей в некоторых случаях стало доходить до 30 тысяч и более. Очевидно, что для ситуаций, когда бот близок к базе будет генерироваться гораздо больше одинаковых путей, чем в случае, когда бот от базы далеко. Вот было бы здорово, если бы в первом случае он тратил мало времени, а во втором побольше! Чтобы этого добиться, я решила исходить из предположения, что если в течение 1500 итераций не удалось <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L36">улучшить решение</a>, то поиск можно прекращать, сэкономив таким образом время. Таким образом, к концу игру удавалось сэкономить довольно много времени, не ухудшив при этом результат.

Когда я делала расчет дистанций для соперников, то сначала использовала уже имеющийся у меня метод <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L58">findPath</a>. Первый же замер показал, что подготовка соперников заняла 200 мс, что было конечно же совершенно недопустимым. Поэтому специально для дистанций соперников был написан <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L133">однопроходный метод просчета</a> с двойной очередью, что позволило сократить время до разумных пределов.

В какой-то момент я заметила, что у меня резко просело число симуляций. Оказалось, что проблема опять в findPath, с помощью которого я проверяла возможность коллизии с соперником. Для решения этой проблемы я просто добавила дополнительную <a href="https://github.com/Oreshnik/paperio/blob/master/src/strategy/Strategy.java#L527">проверку</a> простой дистанцией, без поиска пути, ведь если соперник достаточно далеко, то очевидно, что коллизии произойти не может.

<h2>Технические</h2>
Статические координаты

Очевидно, что как базовая единица, объект координат или точки будет очень часто использоваться ботом, особенно в условиях перебора. Чтобы предотвратить трату ресурсов на генерацию этих объектов и излишнюю сборку мусора, все координаты <a href="https://github.com/Oreshnik/paperio/blob/master/src/model/Point.java#L17">инициализируются</a> при старте игры и далее только <a href="https://github.com/Oreshnik/paperio/blob/master/src/model/Point.java#L33">переиспользуются</a>, без создания новых объектов. Это также позволило использовать более быстрый IdentityHashMap для хранения заливки, хвостов и т.п. 

Отказ от итераторов

Циклы foreach неявно создают объект итератора. В случае высокопроизводительного перебора, создание этих объектов может занимать значительную часть времени. Для моих 2000 итераций это было не очень актуально, но тем не менее, так как эта оптимизация не портит код, я уже по привычке часто заменяю foreach на цикл со счетчиком.

Отказ от values()

Каждый раз, когда мы вызываем метод values() у enum, java из соображений безопасности генерирует новый объект массива. Так как я стараюсь не создавать лишних объектов и сама могу гарантировать безопасность, то если мне нужно перебирать значения, я использую <a href="https://github.com/Oreshnik/paperio/blob/master/src/model/Direction.java#L6">статический массив</a>.

Переиспользование объектов 

Делалось из тех же соображений, что и в предыдущих пунктах, но при этом моем случае было совершенно излишне, так как итераций было слишком мало. 

