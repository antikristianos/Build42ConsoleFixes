require "OptionScreens/ModSelector/ModLoadOrderPanel"

local modLoadOrderPanel = ModSelector and ModSelector.ModLoadOrderPanel
if not modLoadOrderPanel or type(modLoadOrderPanel.autoSort) ~= "function" then
    return
end
local originalAutoSort = modLoadOrderPanel.autoSort
local foreignSortState

local function prioritizeZombieBuddy(items, extraRules)
    local rowsById, duplicate = {}, false
    for _, row in ipairs(items) do
        if rowsById[row.item.modId] then duplicate = true end
        rowsById[row.item.modId] = row
    end
    local buddy = rowsById.ZombieBuddy
    if not buddy then
        return
    end
    if duplicate then
        local ordered = {buddy}
        for _, row in ipairs(items) do if row ~= buddy then ordered[#ordered + 1] = row end end
        for position, row in ipairs(ordered) do items[position] = row end
        print("[ConsoleFixes] Duplicate mod IDs: ZombieBuddy stays first; other rows retain the preceding order.")
        return
    end

    local predecessors, successors, dependents = {}, {}, {}
    for _, row in ipairs(items) do
        local id = row.item.modId
        predecessors[id], successors[id], dependents[id] = {}, {}, {}
    end
    local rootConflict = false
    local function before(first, last, required)
        if not rowsById[first] or not rowsById[last] then return end
        -- The user's fixed root wins over contradictory metadata, including hints.
        if last == "ZombieBuddy" then
            rootConflict = true
            return
        end
        if not predecessors[last][first] then
            predecessors[last][first] = true
            successors[first][#successors[first] + 1] = last
        end
        if required then dependents[first][last] = true end
    end
    for _, row in ipairs(items) do
        local id, info = row.item.modId, row.item.modInfo
        local required = info:getRequire()
        if required then
            for index = 0, required:size() - 1 do
                before(required:get(index), id, true)
            end
        end
        local after = info.getLoadAfter and info:getLoadAfter()
        if after then
            for index = 0, after:size() - 1 do before(after:get(index), id) end
        end
        local ahead = info.getLoadBefore and info:getLoadBefore()
        if ahead then
            for index = 0, ahead:size() - 1 do before(id, ahead:get(index)) end
        end
        -- The foreign sorter's resolved cache includes user sorting-rule overrides.
        local extra = extraRules and extraRules[id]
        if extra then
            for _, dependency in ipairs(extra.requirements or {}) do before(dependency, id, true) end
            local rules = extra.sortingRules or {}
            for _, dependency in ipairs(rules.loadAfter or {}) do before(dependency, id) end
            for _, following in ipairs(rules.loadBefore or {}) do before(id, following) end
        end
    end

    local group = {ZombieBuddy = true}
    local queue = {"ZombieBuddy"}
    local function include(id)
        if not group[id] then
            group[id] = true
            queue[#queue + 1] = id
        end
    end
    -- Seed the block with mandatory Buddy consumers, not every optional follower.
    local index = 1
    while index <= #queue do
        for id in pairs(dependents[queue[index]]) do include(id) end
        index = index + 1
    end
    -- Pull in all active predecessors, including optional hints and their chains.
    index = 1
    while index <= #queue do
        for id in pairs(predecessors[queue[index]]) do include(id) end
        index = index + 1
    end

    local ordered = {buddy}
    local emitted, pending = {ZombieBuddy = true}, {}
    for id, incoming in pairs(predecessors) do
        local count = 0
        for dependency in pairs(incoming) do
            if dependency ~= "ZombieBuddy" then count = count + 1 end
        end
        pending[id] = count
    end
    local cycle = false
    for _, preferred in ipairs({true, false}) do
        while true do
            local selected, remaining
            for _, row in ipairs(items) do
                local id = row.item.modId
                if not emitted[id] and (group[id] == true) == preferred then
                    remaining = true
                    if pending[id] == 0 then selected = row; break end
                end
            end
            if not remaining then break end
            if not selected then cycle = true; break end
            local id = selected.item.modId
            emitted[id] = true
            ordered[#ordered + 1] = selected
            for _, following in ipairs(successors[id]) do pending[following] = pending[following] - 1 end
        end
        if cycle then break end
    end
    if cycle then
        -- Never expose a partly sorted result or lose rows on inconsistent metadata.
        ordered = {buddy}
        for _, row in ipairs(items) do if row ~= buddy then ordered[#ordered + 1] = row end end
        print("[ConsoleFixes] Mod-order cycle: ZombieBuddy stays first; other rows retain the preceding order.")
    end
    if rootConflict then
        print("[ConsoleFixes] Mod-order metadata conflicts with ZombieBuddy's fixed first position; that position takes precedence.")
    end
    for position, row in ipairs(ordered) do
        items[position] = row
    end
end

local function autoSort(self, ...)
    local beforeForeignSort = foreignSortState and foreignSortState.serial
    originalAutoSort(self, ...)

    local items = self.modList and self.modList.items
    if type(items) ~= "table" then
        return
    end

    -- Vanilla includes inactive load-order references, then maps them to nil.
    -- Enumerate every surviving row: ipairs and # cannot cross these holes.
    local indices = {}
    for index in pairs(items) do
        if type(index) == "number" and index >= 1 and index % 1 == 0 then
            indices[#indices + 1] = index
        end
    end
    table.sort(indices)

    -- Compact in place, retaining Vanilla order, row objects and table identity.
    for targetIndex, sourceIndex in ipairs(indices) do
        if targetIndex ~= sourceIndex then
            items[targetIndex] = items[sourceIndex]
            items[sourceIndex] = nil
        end
    end
    local rules = foreignSortState and foreignSortState.serial ~= beforeForeignSort and foreignSortState.rules
    prioritizeZombieBuddy(items, rules or nil)
end

if not modLoadOrderPanel.build42ConsoleFixesAutoSort then
    modLoadOrderPanel.autoSort = autoSort
    modLoadOrderPanel.build42ConsoleFixesAutoSort = autoSort
end

local function installForeignSorter()
    local mods = getActivatedMods ~= nil and getActivatedMods() or nil
    if mods == nil or not mods:contains("ModLoadOrderSorter_b42") then return end
    local loaded, sorter = pcall(require, "OptionScreens/ModSelector/MLOS_sorting")
    if not loaded or type(sorter) ~= "table" or type(sorter.SortModsOrder) ~= "function" then return end
    if type(sorter.build42ConsoleFixesSortState) ~= "table" then
        sorter.build42ConsoleFixesSortState = {serial = 0}
    end
    foreignSortState = sorter.build42ConsoleFixesSortState
    if sorter.SortModsOrder == sorter.build42ConsoleFixesSort then return end
    local state = foreignSortState
    local previousSort = sorter.SortModsOrder
    local function sort(self, rows, ...)
        local orderedIds = previousSort(self, rows, ...)
        if type(rows) ~= "table" or type(orderedIds) ~= "table" then return orderedIds end
        local byId = {}
        for _, row in ipairs(rows) do
            if row.item ~= nil then byId[row.item.modId] = row end
        end
        local orderedRows, seen = {}, {}
        for _, id in ipairs(orderedIds) do
            local row = byId[id]
            -- Preserve an unsupported preceding result instead of guessing rows.
            if row == nil or seen[id] then return orderedIds end
            seen[id] = true
            orderedRows[#orderedRows + 1] = row
        end
        state.serial = state.serial + 1
        state.rules = sorter.modsInfoCache
        prioritizeZombieBuddy(orderedRows, state.rules)
        for index, row in ipairs(orderedRows) do orderedIds[index] = row.item.modId end
        return orderedIds
    end
    sorter.SortModsOrder = sort
    sorter.build42ConsoleFixesSort = sort
end

installForeignSorter()
if Events ~= nil and not modLoadOrderPanel.build42ConsoleFixesForeignInstaller then
    modLoadOrderPanel.build42ConsoleFixesForeignInstaller = installForeignSorter
    for _, event in ipairs({ Events.OnGameBoot, Events.OnMainMenuEnter, Events.OnGameStart }) do
        if event ~= nil then event.Add(installForeignSorter) end
    end
end
