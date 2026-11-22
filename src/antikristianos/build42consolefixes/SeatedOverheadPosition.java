package antikristianos.build42consolefixes;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.ui.ActionProgressBar;

/** Keep DAMN's temporary render-order height out of the action-bar projection only. */
public final class SeatedOverheadPosition {
    private SeatedOverheadPosition() { }
    public static void position(ActionProgressBar bar,int playerIndex) {
        if(GameServer.server||playerIndex<0||playerIndex>=IsoPlayer.players.length)return;
        IsoPlayer player=IsoPlayer.players[playerIndex];
        if(player==null||player.getVehicle()==null||LuaManager.env==null)return;
        Object value=LuaManager.env.rawget("DAMN");
        if(!(value instanceof KahluaTable damn))return;
        value=damn.rawget("DepthSorting");if(!(value instanceof KahluaTable sorting))return;
        value=sorting.rawget("AppliedFloorZ");if(!(value instanceof KahluaTable floors))return;
        value=floors.rawget(player);if(!(value instanceof Number number))return;
        float floor=number.floatValue(),actual=player.getZ();
        if(!Float.isFinite(floor)||!Float.isFinite(actual)||floor==actual
                ||floor!=(float)Math.floor(player.getVehicle().getZ()))return;
        float zoom=Core.getInstance().getZoom(playerIndex);
        if(!Float.isFinite(zoom)||zoom<=0)return;
        float x=player.getX(),y=player.getY();
        bar.setX(bar.getX()+(IsoUtils.XToScreen(x,y,floor,0)-IsoUtils.XToScreen(x,y,actual,0))/zoom);
        bar.setY(bar.getY()+(IsoUtils.YToScreen(x,y,floor,0)-IsoUtils.YToScreen(x,y,actual,0))/zoom);
    }
}
