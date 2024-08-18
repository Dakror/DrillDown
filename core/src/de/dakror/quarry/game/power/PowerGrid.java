package de.dakror.quarry.game.power;

import com.badlogic.gdx.utils.Array;

import de.dakror.quarry.structure.base.Structure;

public class PowerGrid {
    private Array<PowerNetwork> networks;

    public PowerGrid() {
        this.networks = new Array<>();
    }

    public Array<PowerNetwork> getNetworks() {
        return networks;
    }

    public void update(double deltaTime, int gameSpeed) {
        for (PowerNetwork n : networks) {
            n.update(deltaTime, gameSpeed);
        }
    }

    public void clearHighPowerCache() {
        for (int i = 0; i < networks.size; i++) {
            networks.get(i).clearHighPowerCache();
        }
    }

    public void clear() {
        networks.clear();
    }

    public void addNetwork(PowerNetwork network) {
        networks.add(network);
        clearHighPowerCache();
    }

    public void removeNetwork(PowerNetwork network) {
        networks.removeValue(network, true);
        clearHighPowerCache();
    }

    public boolean mergeNetworks(PowerNetwork a, PowerNetwork b) {
        if (a == b) {
            return false;
        }

        if (!b.hasEdges()) {
            Structure<?> vert = b.getFirstVertex();
            if (vert != null)
                a.addVertex(vert);
        }

        for (Edge e : b.getEdges()) {
            a.addEdge(e);
        }

        removeNetwork(b);

        return true;
    }

    @Override
    public String toString() {
        return networks.toString();
    }
}
