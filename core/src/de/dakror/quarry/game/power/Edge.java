package de.dakror.quarry.game.power;

import java.util.Objects;

import de.dakror.quarry.game.power.PowerNetwork.NetworkStrength;
import de.dakror.quarry.structure.base.Structure;

public class Edge {
    private Structure<?> a, b;
    private int dockA, dockB;
    private NetworkStrength networkStrength;

    protected Edge(Structure<?> a, int dockA, Structure<?> b, int dockB, NetworkStrength networkStrength) {
        this.a = a;
        this.b = b;
        this.dockA = dockA;
        this.dockB = dockB;
        this.networkStrength = networkStrength;
    }

    public NetworkStrength getNetworkStrength() {
        return networkStrength;
    }

    public int getDockA() {
        return dockA;
    }

    public int getDockB() {
        return dockB;
    }

    public Structure<?> getOther(Structure<?> s) {
        if (s.equals(a)) return b;
        if (s.equals(b)) return a;
        return null;
    }

    public Structure<?> getA() {
        return a;
    }

    public Structure<?> getB() {
        return b;
    }

    public void setA(Structure<?> a) {
        this.a = a;
    }

    public void setB(Structure<?> b) {
        this.b = b;
    }

    public void setDockA(int dockA) {
        this.dockA = dockA;
    }

    public void setDockB(int dockB) {
        this.dockB = dockB;
    }

    @Override
    public int hashCode() {
        // both directions
        return Objects.hash(a, dockA, b, dockB) + Objects.hash(b, dockB, a, dockA);
    }

    @Override
    public String toString() {
        return String.format("%d:%d <-> %d:%d", a.getIndex(), dockA, b.getIndex(), dockB);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Edge) {
            return obj.hashCode() == hashCode();
        } else {
            return false;
        }
    }
}
