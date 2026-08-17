execute if items entity @s weapon.mainhand minecraft:splash_potion[minecraft:potion_contents~"minecraft:strong_strength"] run item modify entity @s weapon.mainhand eightminutestrength:set_duration_scale
execute if items entity @s weapon.mainhand minecraft:lingering_potion[minecraft:potion_contents~"minecraft:strong_strength"] run item modify entity @s weapon.mainhand eightminutestrength:set_duration_scale
execute if items entity @s weapon.offhand minecraft:splash_potion[minecraft:potion_contents~"minecraft:strong_strength"] run item modify entity @s weapon.offhand eightminutestrength:set_duration_scale
execute if items entity @s weapon.offhand minecraft:lingering_potion[minecraft:potion_contents~"minecraft:strong_strength"] run item modify entity @s weapon.offhand eightminutestrength:set_duration_scale
advancement revoke @s only eightminutestrength:use_thrown_strong_strength
