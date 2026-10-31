require "OptionScreens/ModSelector/ModLoadOrderPanel"

local modLoadOrderPanel = ModSelector and ModSelector.ModLoadOrderPanel
if not modLoadOrderPanel or type(modLoadOrderPanel.autoSort) ~= "function" then
    return
end
if modLoadOrderPanel.build42ConsoleFixesAutoSort then
    return
end

local originalAutoSort = modLoadOrderPanel.autoSort

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
end

modLoadOrderPanel.autoSort = autoSort
modLoadOrderPanel.build42ConsoleFixesAutoSort = autoSort
