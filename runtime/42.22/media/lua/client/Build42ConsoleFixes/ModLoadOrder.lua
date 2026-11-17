require "OptionScreens/ModSelector/ModLoadOrderPanel"

local modLoadOrderPanel = ModSelector and ModSelector.ModLoadOrderPanel
if not modLoadOrderPanel or type(modLoadOrderPanel.autoSort) ~= "function" then
    return
end
if modLoadOrderPanel.build42ConsoleFixesAutoSort then
    return
end

local originalAutoSort = modLoadOrderPanel.autoSort

local function prioritizeZombieBuddy(items)
    local rowsById = {}
    for _, row in ipairs(items) do
        rowsById[row.item.modId] = row
    end
    local buddy = rowsById.ZombieBuddy
    if not buddy then
        return
    end

    local requirements = {}
    local dependents = {}
    for _, row in ipairs(items) do
        local id = row.item.modId
        local required = row.item.modInfo:getRequire()
        requirements[id] = {}
        if required then
            for index = 0, required:size() - 1 do
                local dependency = required:get(index)
                if rowsById[dependency] then
                    table.insert(requirements[id], dependency)
                    dependents[dependency] = dependents[dependency] or {}
                    table.insert(dependents[dependency], id)
                end
            end
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
    -- Follow required dependencies only; optional load-order hints are not dependencies.
    local index = 1
    while index <= #queue do
        for _, id in ipairs(dependents[queue[index]] or {}) do
            include(id)
        end
        index = index + 1
    end
    -- Keep any other required libraries ahead of the mods that need them.
    index = 1
    while index <= #queue do
        for _, id in ipairs(requirements[queue[index]]) do
            include(id)
        end
        index = index + 1
    end

    local ordered = {buddy}
    for _, row in ipairs(items) do
        if row ~= buddy and group[row.item.modId] then
            ordered[#ordered + 1] = row
        end
    end
    for _, row in ipairs(items) do
        if not group[row.item.modId] then
            ordered[#ordered + 1] = row
        end
    end
    for position, row in ipairs(ordered) do
        items[position] = row
    end
end

local function autoSort(self, ...)
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
    prioritizeZombieBuddy(items)
end

modLoadOrderPanel.autoSort = autoSort
modLoadOrderPanel.build42ConsoleFixesAutoSort = autoSort
