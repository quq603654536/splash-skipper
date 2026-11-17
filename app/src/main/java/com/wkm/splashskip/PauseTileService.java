package com.wkm.splashskip;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
public final class PauseTileService extends TileService {
    @Override public void onStartListening(){super.onStartListening();refresh();}
    @Override public void onClick(){super.onClick();Store s=new Store(this);s.enabled(!s.enabled());refresh();}
    private void refresh(){Tile t=getQsTile();if(t!=null){boolean on=new Store(this).enabled();t.setState(on?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);t.setLabel(on?"开屏快跳":"快跳已暂停");t.updateTile();}}
}
