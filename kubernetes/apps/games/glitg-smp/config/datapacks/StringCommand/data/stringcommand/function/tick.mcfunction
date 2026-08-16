scoreboard players enable @a stringcommand
execute as @a[scores={stringcommand=1..}] run function stringcommand:fill
scoreboard players reset @a[scores={stringcommand=1..}] stringcommand
