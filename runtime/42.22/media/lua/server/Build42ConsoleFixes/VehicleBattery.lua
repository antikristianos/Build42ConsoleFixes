require "Vehicles/Vehicles"

-- Vanilla applies delta twice. Keep its guards, clamping and transmission path.
if not VehicleUtils.consoleFixesBatteryDelta then
    VehicleUtils.consoleFixesBatteryDelta = true
    local originalChargeBattery = VehicleUtils.chargeBattery
    local originalBatteryUpdate = Vehicles.Update.Battery
    local originalHeaterUpdate = Vehicles.Update.Heater
    local originalCompartmentUpdate = Vehicles.Update.PassengerCompartment
    local heaterVehicle

    function VehicleUtils.chargeBattery(vehicle, delta)
        -- The running heater is included in the alternator's net output below.
        if vehicle == heaterVehicle and type(delta) == "number" and delta < 0
                and vehicle:isEngineRunning() then return end
        return originalChargeBattery(vehicle, type(delta) == "number" and delta * 0.5 or delta)
    end

    local function runningLoad(vehicle)
        local rate = 0
        for index = 0, vehicle:getPartCount() - 1 do
            local part = vehicle:getPartByIndex(index)
            local update = part:getLuaFunction("update")
            if update == "Vehicles.Update.Headlight" then
                if vehicle:getHeadlightsOn() and part:getLight() and part:getInventoryItem() then
                    rate = rate + 0.000025
                end
            elseif update == "Vehicles.Update.Radio" then
                local device = part:getDeviceData()
                if device and device:getIsTurnedOn() then rate = rate + 0.000025 end
            elseif update == "Vehicles.Update.Heater" then
                if part:getModData().active and vehicle:getPartById("PassengerCompartment")
                        and vehicle:getPartById("Engine") then rate = rate + 0.000035 end
            end
        end
        if vehicle:hasLightbar() then
            if vehicle:getLightbarLightsMode() > 0 then rate = rate + 0.000025 end
            if vehicle:getLightbarSirenMode() > 0 and not vehicle:sirenShutoffTimeExpired() then
                rate = rate + 0.000025
            end
        end
        return rate
    end

    function Vehicles.Update.Battery(vehicle, part, elapsedMinutes)
        if vehicle:isEngineRunning() and elapsedMinutes > 0 then
            -- Keep the native charging helper, but supply only generation left after loads.
            elapsedMinutes = elapsedMinutes * math.max(0, 1 - runningLoad(vehicle) / 0.001)
        end
        return originalBatteryUpdate(vehicle, part, elapsedMinutes)
    end

    function Vehicles.Update.Heater(vehicle, part, elapsedMinutes)
        local previous = heaterVehicle
        heaterVehicle = vehicle
        local success, result = pcall(originalHeaterUpdate, vehicle, part, elapsedMinutes)
        heaterVehicle = previous
        if not success then error(result) end
        return result
    end

    function Vehicles.Update.PassengerCompartment(vehicle, part, elapsedMinutes)
        local heater = vehicle:getHeater()
        local data = heater and heater:getModData()
        if vehicle:isEngineRunning() or not data or not data.active then
            return originalCompartmentUpdate(vehicle, part, elapsedMinutes)
        end
        -- Native window-temperature behavior also inspects the heater switch.
        -- Treat it as off while stopped, without losing the player's setting.
        local active = data.active
        data.active = false
        local success, result = pcall(originalCompartmentUpdate, vehicle, part, elapsedMinutes)
        data.active = active
        if not success then error(result) end
        return result
    end
end
