require "BuildingObjects/ISBuildIsoEntity"

if not ISBuildIsoEntity.consoleFixesFloorPlacement then
    ISBuildIsoEntity.consoleFixesFloorPlacement = true
    local originalSetInfo = ISBuildIsoEntity.setInfo

    function ISBuildIsoEntity:setInfo(square, north, sprite, openSprite)
        local bridge = ConsoleFixesFloorPlacement
        local script = self.objectInfo and self.objectInfo:getScript()
        if not bridge or not square or not script
            or script:getOnCreate() ~= "BuildRecipeCode.floor.OnCreate" then
            return originalSetInfo(self, square, north, sprite, openSprite)
        end

        local scope = bridge("begin", square)
        local success, result = pcall(originalSetInfo, self, square, north, sprite, openSprite)
        local closed, closeError = pcall(bridge, "close", scope)
        if not success then error(result) end
        if not closed then error(closeError) end
        return result
    end
end
