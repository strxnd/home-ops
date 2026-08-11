glitg_legendary_chestplate:
  type: item
  material: netherite_chestplate
  display name: <gold>GLITG Legendary Netherite Chestplate
  lore:
    - <gray>One of one
    - <dark_gray>Cannot be stored or destroyed
  mechanisms:
    unbreakable: true
    rarity: epic
  recipes:
    1:
      type: shaped
      recipe_id: legendary_netherite_chestplate
      input:
        - material:diamond|material:diamond|material:diamond
        - material:diamond|material:dragon_egg|material:diamond
        - material:diamond|material:diamond|material:diamond

glitg_legendary_mace:
  type: item
  material: mace
  display name: <gold>GLITG Legendary Mace
  lore:
    - <gray>One of one
    - <dark_gray>Cannot be stored or destroyed
  mechanisms:
    rarity: epic

glitg_strength_two:
  type: item
  material: potion
  display name: <light_purple>Potion of Strength
  mechanisms:
    potion_effects:
      - "[translation_id=strength]"
      - "[effect=strength;amplifier=1;duration=8m;ambient=false;particles=true;icon=true]"
  recipes:
    1:
      type: brewing
      recipe_id: strength_two_eight_minutes
      input: "potion[potion_effects=[base_type=strong_strength]]"
      ingredient: material:redstone

glitg_is_protected_item:
  type: procedure
  definitions: item
  script:
    - if <[item].script.name.if_null[null]> == glitg_legendary_chestplate:
      - determine true
    - if <[item].script.name.if_null[null]> == glitg_legendary_mace:
      - determine true
    - define public_data <[item].custom_data.get[PublicBukkitValues].if_null[<map>]>
    - if <[public_data].get[lifestealz:customitemtype].if_null[null]> == heart:
      - determine true
    - determine false

glitg_rules:
  type: world
  events:
    on server start:
      - if !<server.has_flag[glitg_phase]>:
        - flag server glitg_phase:week1
      - if !<server.has_flag[glitg_locator]>:
        - foreach <server.worlds> as:world:
          - gamerule <[world]> locator_bar false
      - foreach <server.worlds> as:world:
        - worldborder <[world]> size:4000

    after world loads:
      - worldborder <context.world> size:4000
      - if !<server.has_flag[glitg_locator]>:
        - gamerule <context.world> locator_bar false

    on player damages player:
      - if <server.has_flag[glitg_grace]>:
        - determine cancelled
      - if <player.has_flag[glitg_respawn_protection]> || <context.entity.has_flag[glitg_respawn_protection]>:
        - determine cancelled

    on player dies:
      - flag player glitg_respawn_protection expire:30m
      - if <player.has_effect[invisibility]>:
        - determine no_message
      - if <context.damager.is_player.if_null[false]> && <context.damager.has_effect[invisibility]>:
        - determine no_message

    on player equips armor:
      - flag player glitg_respawn_protection:!

    on player teleports cause:ender_pearl:
      - determine cancelled

    on player uses portal:
      - if <context.to.world.environment.if_null[null]> == THE_END && <server.has_flag[glitg_end_closed]>:
        - narrate "<red>The End is closed until week two."
        - determine cancelled

    on player damaged with:mace:
      - if <context.damage> > 16:
        - determine 16

    on player damaged by tnt_minecart:
      - if <context.damage> > 20:
        - determine 20

    on mace recipe formed:
      - determine glitg_legendary_mace

    on player clicks in inventory:
      - if <context.inventory.inventory_type> != CRAFTING:
        - if <proc[glitg_is_protected_item].context[<context.item>]> || <proc[glitg_is_protected_item].context[<context.cursor_item>]>:
          - determine cancelled
      - wait 1t
      - run glitg_enforce_healing_limits def:<player>

    on player drags in inventory:
      - if <context.inventory.inventory_type> != CRAFTING && <proc[glitg_is_protected_item].context[<context.item>]>:
        - determine cancelled

    on item moves from inventory to inventory:
      - if <proc[glitg_is_protected_item].context[<context.item>]>:
        - determine cancelled

    on inventory picks up item:
      - if <proc[glitg_is_protected_item].context[<context.item>]>:
        - determine cancelled

    on player right clicks item_frame:
      - if <proc[glitg_is_protected_item].context[<player.item_in_hand>]>:
        - determine cancelled

    on glitg_legendary_mace despawns:
      - determine cancelled

    on glitg_legendary_chestplate despawns:
      - determine cancelled

    on item damaged:
      - if <proc[glitg_is_protected_item].context[<context.entity.item>]>:
        - determine cancelled

    after player picks up item:
      - run glitg_enforce_healing_limits def:<player>

    after player joins:
      - run glitg_enforce_healing_limits def:<player>

    on system time minutely:
      - if !<server.has_flag[glitg_locator]>:
        - foreach <server.worlds> as:world:
          - gamerule <[world]> locator_bar false

glitg_enforce_healing_limits:
  type: task
  definitions: target
  script:
    - define counts <map>
    - foreach <[target].inventory.list_contents> key:slot as:item:
      - define material <[item].material.name>
      - if !<[material].is_in[potion|splash_potion|lingering_potion]>:
        - foreach next
      - if <[item].effects_data.get[1].get[base_type].if_null[null]> != healing:
        - foreach next
      - define count <[counts].get[<[material]>].if_null[0]>
      - define room <element[6].sub[<[count]>]>
      - if <[room]> <= 0:
        - inventory set slot:<[slot]> o:air destination:<[target].inventory
      - else if <[item].quantity> > <[room]>:
        - inventory set slot:<[slot]> o:<[item].with_quantity[<[room]>]> destination:<[target].inventory
      - define counts.<[material]>:<[count].add[<[item].quantity>].min[6]>

glitg_command:
  type: command
  name: glitg
  description: Manage the GLITG season state.
  usage: /glitg status|start|week1|week2|final_day|grace|end|locator
  permission: glitg.admin
  permission message: <red>Operator access required.
  script:
    - define action <context.args.get[1].if_null[status].to_lowercase>
    - choose <[action]>:
      - case status:
        - narrate "<gold>phase=<server.flag[glitg_phase].if_null[week1]>, grace=<server.has_flag[glitg_grace]>, end_closed=<server.has_flag[glitg_end_closed]>, locator=<server.has_flag[glitg_locator]>"
      - case start:
        - flag server glitg_phase:week1
        - flag server glitg_grace expire:60m
        - flag server glitg_end_closed expire:7d
        - execute as_server "ce enable lifesteal_week1_floor" silent
        - execute as_server "ce disable lifesteal_week2_floor" silent
        - announce "<gold>GLITG S1 has started. Global PvP grace is active for 60 minutes."
      - case week1:
        - flag server glitg_phase:week1
        - execute as_server "ce enable lifesteal_week1_floor" silent
        - execute as_server "ce disable lifesteal_week2_floor" silent
      - case week2:
        - flag server glitg_phase:week2
        - flag server glitg_end_closed:!
        - execute as_server "ce disable lifesteal_week1_floor" silent
        - execute as_server "ce enable lifesteal_week2_floor" silent
        - announce "<gold>Week two is active. The heart floor is one and the End is open."
      - case final_day:
        - flag server glitg_phase:final_day
        - execute as_server "ce disable lifesteal_week1_floor" silent
        - execute as_server "ce disable lifesteal_week2_floor" silent
        - announce "<red>Final day is active. Zero-heart elimination is enabled."
      - case grace:
        - if <context.args.get[2].if_null[off]> == on:
          - flag server glitg_grace expire:60m
        - else:
          - flag server glitg_grace:!
      - case end:
        - if <context.args.get[2].if_null[open]> == open:
          - flag server glitg_end_closed:!
        - else:
          - flag server glitg_end_closed
      - case locator:
        - define enabled <context.args.get[2].if_null[off].equals[on]>
        - if <[enabled]>:
          - flag server glitg_locator expire:24h
        - else:
          - flag server glitg_locator:!
        - foreach <server.worlds> as:world:
          - gamerule <[world]> locator_bar <[enabled]>
      - default:
        - narrate "<red>Usage: <script[glitg_command].data_key[usage]>"
