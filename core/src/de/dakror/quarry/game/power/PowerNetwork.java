package de.dakror.quarry.game.power;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.OrderedSet;

import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.power.CopperCable;
import de.dakror.quarry.structure.power.PowerPole;
import de.dakror.quarry.structure.power.PowerPoleGhost;
import de.dakror.quarry.structure.power.Substation;

public class PowerNetwork {
    public static enum NetworkStrength {
        CopperCable(450_000),
        PowerPole(1_000_000_000),

        ;

        public final double maxPowerPerSecond;

        private NetworkStrength(double pps) {
            this.maxPowerPerSecond = pps;
        }
    }

    private PowerGrid grid;

    private Set<Edge> edgeSet;
    private IntMap<Edge[]> edges;

    // cached complete graph for fast lookup of connections. 
    // recalculated when a high power member of this network is modified
    private IntMap<IntMap<Boolean>> highPowerConnections;

    // structures with power storage
    private OrderedSet<Structure<?>> donorVertices;
    // structures that consume power
    private OrderedSet<Structure<?>> receiverVertices;
    // structures without battery and consumption, so Generators
    private OrderedSet<Structure<?>> neutralVertices;

    private OrderedSet<Structure<?>> allVertices;

    private int numLowReceiverSlices;
    private int numHighReceiverSlices;

    private boolean hadEdgeDeletion;

    private final WindowedMean inTickMean = new WindowedMean(60);
    private final WindowedMean outTickMean = new WindowedMean(60);
    private final WindowedMean timeTickMean = new WindowedMean(60);

    double inTick, outTick;

    double accumulatedOfferedPower;

    static int idCounter = 0;

    int id;

    final IntSet dfsScratch = new IntSet();

    Array<Edge> minimumSpanningTree = new Array<Edge>();
    boolean minimumSpanningTreeDirty = true;

    public PowerNetwork(PowerGrid grid) {
        this.grid = grid;

        this.id = idCounter++;

        edges = new IntMap<>();
        edgeSet = new HashSet<>();

        neutralVertices = new OrderedSet<>();
        allVertices = new OrderedSet<>();
        highPowerConnections = new IntMap<>();

        donorVertices = new OrderedSet<>();
        /*new Comparator<Structure<?>>() {
            @Override
            public int compare(Structure<?> a, Structure<?> b) {
                int dp = b.getDonorPriority() - a.getDonorPriority();
                if (dp == 0) return a.hashCode() - b.hashCode();
                else return dp;
            }
        });*/

        receiverVertices = new OrderedSet<>();
        /*new Comparator<Structure<?>>() {
            @Override
            public int compare(Structure<?> a, Structure<?> b) {
                int dp = b.getReceiverPriority() - a.getReceiverPriority();
                if (dp == 0) return a.hashCode() - b.hashCode();
                else return dp;
            }
        });*/
    }

    public Structure<?> getFirstVertex() {
        if (allVertices.size == 0) return null;

        return allVertices.iterator().next();
    }

    public boolean hasVertex(Structure<?> s) {
        return allVertices.contains(s);
    }

    public PowerGrid getPowerGrid() {
        return grid;
    }

    public Set<Edge> getEdges() {
        return edgeSet;
    }

    public boolean addVertex(Structure<?> node) {
        if (!allVertices.add(node)) {
            return false;
        }

        if (node.getDonorPriority() > 0) {
            if (!donorVertices.add(node)) return false;
        }

        if (node.getReceiverPriority() > 0) {
            if (!receiverVertices.add(node)) return false;

            if (node.getSchema().highPower)
                numHighReceiverSlices += node.getReceiverPriority();

            if (node.getSchema().lowPower)
                numLowReceiverSlices += node.getReceiverPriority();
        }

        if (node.getDonorPriority() == 0 && node.getReceiverPriority() == 0) {
            if (!neutralVertices.add(node)) return false;
        }

        if (!edges.containsKey(node.getIndex())) {
            edges.put(node.getIndex(), new Edge[node.getSchema().powerDocks]);
        }

        if (node.getSchema().highPower && !highPowerConnections.containsKey(node.getIndex())) {
            highPowerConnections.put(node.getIndex(), new IntMap<Boolean>());
        }

        node.setPowerNetwork(this);
        minimumSpanningTreeDirty = true;
        return true;
    }

    public Edge addEdge(Edge e) {
        return addEdge(e.getA(), e.getDockA(), e.getB(), e.getDockB(), e.getNetworkStrength());
    }

    public Edge addEdge(Structure<?> a, int dockA, Structure<?> b, int dockB, NetworkStrength networkStrength) {
        if (a == b) return null;

        int keyA = a.getIndex();
        Edge[] edgesOfA = edges.get(keyA);

        int keyB = b.getIndex();
        Edge[] edgesOfB = edges.get(keyB);

        if ((edgesOfA == null || edgesOfA[dockA] == null) && (edgesOfB == null || edgesOfB[dockB] == null)) {
            Edge e = new Edge(a, dockA, b, dockB, networkStrength);

            edgeSet.add(e);

            addVertex(a);
            addVertex(b);

            edgesOfA = edges.get(keyA);
            edgesOfA[dockA] = e;
            edges.put(keyA, edgesOfA);

            edgesOfB = edges.get(keyB);
            edgesOfB[dockB] = e;
            edges.put(keyB, edgesOfB);

            if (networkStrength == NetworkStrength.PowerPole) {
                IntMap<Boolean> connectionsA = highPowerConnections.get(keyA);
                connectionsA.put(keyA, true);
                IntMap<Boolean> connectionsB = highPowerConnections.get(keyB);
                connectionsB.put(keyB, true);

                grid.clearHighPowerCache();
            }

            return e;
        }

        return null;
    }

    public boolean removeVertex(Structure<?> node, boolean createIslandNetworks) {
        if (!allVertices.remove(node)) {
            return false;
        }
        if (node.getDonorPriority() > 0) {
            if (!donorVertices.remove(node)) return false;
        }
        if (node.getReceiverPriority() > 0) {
            if (!receiverVertices.remove(node)) return false;

            if (node.getSchema().highPower)
                numHighReceiverSlices += node.getReceiverPriority();

            if (node.getSchema().lowPower)
                numLowReceiverSlices += node.getReceiverPriority();
        }
        if (node.getDonorPriority() == 0 && node.getReceiverPriority() == 0) {
            if (!neutralVertices.remove(node)) return false;
        }

        Edge[] edgesToRemove = edges.get(node.getIndex());

        for (Edge e : edgesToRemove) {
            if (e == null) continue;

            removeEdge(e, createIslandNetworks);
        }

        edges.remove(node.getIndex());

        return true;
    }

    public void offerPower(double deltaTime, int gameSpeed, double power, Structure<?> donor) {
        if (donor.getSchema().highPower) {
            // high power has to be distributed per building
            // because its not known where it could go at what speed
            double remainder = power;

            int donorPriority = donor.getDonorPriority();

            int receiverSlices = numHighReceiverSlices;

            for (int i = 0; i < 5; i++) {
                if (remainder == 0) break;
                for (Structure<?> n : receiverVertices) {
                    if (donorPriority > 0 && (n.getDonorPriority() > donorPriority)) continue;

                    double piece = Math.min(remainder / receiverSlices * n.getReceiverPriority(), remainder);

                    // lookup if high power connection exists     
                    NetworkStrength strength = getConnectionNetworkStrength(donor, n);
                    if (strength == null) continue;

                    double networkStrength = strength.maxPowerPerSecond * deltaTime * gameSpeed;

                    double leftover = n.acceptPower(piece, networkStrength);

                    if (!(n instanceof Substation)) {
                        outTick += piece - leftover;
                    }

                    remainder = remainder - piece + leftover;

                    if (remainder == 0) break;
                }
            }
            inTick += power - remainder;
        } else {
            // instead of distributing each offered amount individually,
            // we collect it all. Donor priority is always 0 for generators
            accumulatedOfferedPower += power;
        }
    }

    private void distributeOfferedLowPower(double deltaTime, int gameSpeed) {
        double networkStrength = NetworkStrength.CopperCable.maxPowerPerSecond * deltaTime * gameSpeed;
        double remainder = accumulatedOfferedPower;

        int receiverSlices = numLowReceiverSlices;

        for (int i = 0; i < 5; i++) {
            if (remainder == 0) break;
            for (Structure<?> n : receiverVertices) {
                if (remainder == 0) break;

                double piece = Math.min(remainder / receiverSlices * n.getReceiverPriority(), remainder);
                double leftover = n.acceptPower(piece, networkStrength);

                if (!(n instanceof Substation)) {
                    outTick += piece - leftover;
                }

                remainder = remainder - piece + leftover;
                //receiverSlices -= n.getReceiverPriority();

                if (remainder == 0) break;
            }
        }

        inTick += accumulatedOfferedPower - remainder;

        // reset amount offered
        accumulatedOfferedPower = remainder;
    }

    private void distributeStoredPower(double deltaTime, int gameSpeed) {
        for (Structure<?> r : receiverVertices) {
            double delta = r.getPowerCapacity() - r.getPowerLevel();

            if (delta > 0) {
                double remainingRequest = delta;

                for (Structure<?> d : donorVertices) {
                    if (remainingRequest == 0) break;

                    if (d == r) continue;

                    if (d.getReceiverPriority() > r.getReceiverPriority()
                            || (d.getReceiverPriority() == r.getReceiverPriority()
                                    && r.getPowerRatio() >= d.getPowerRatio())) {
                        continue;
                    }

                    double request = remainingRequest;

                    if (d.getReceiverPriority() == r.getReceiverPriority()) {
                        request = Math.min((d.getPowerRatio() - r.getPowerRatio()) / 2 * r.getPowerCapacity(), request);
                    }

                    // skip super small differences
                    if (request < 1) continue;

                    // lookup if high power connection exists

                    NetworkStrength strength = getConnectionNetworkStrength(d, r);
                    if (strength == null) continue;

                    double networkStrength = strength.maxPowerPerSecond * deltaTime * gameSpeed;

                    request = Math.min(request, networkStrength);

                    double gotten = d.requestPower(request, networkStrength);

                    if (gotten > 0) {
                        double leftover = r.acceptPower(gotten, networkStrength);

                        // refund the leftover to the donor
                        d.refundPower(leftover);

                        if (!(r instanceof Substation)) {
                            outTick += gotten - leftover;
                        }

                        remainingRequest -= gotten - leftover;
                    }
                }
            }
        }
    }

    public void update(double deltaTime, int gameSpeed) {
        if (isEmpty()) {
            grid.removeNetwork(this);
            return;
        } else if (hadEdgeDeletion) {
            manageConnectedness();
            hadEdgeDeletion = false;
        }

        if (gameSpeed == 0) return;

        timeTickMean.addValue((float) deltaTime);

        if (accumulatedOfferedPower > 0)
            distributeOfferedLowPower(deltaTime, gameSpeed);
        distributeStoredPower(deltaTime, gameSpeed);

        inTickMean.addValue((float) inTick);
        outTickMean.addValue((float) outTick);
        inTick = 0;
        outTick = 0;
    }

    public NetworkStrength getConnectionNetworkStrength(Structure<?> a, Structure<?> b) {
        if (!a.getSchema().highPower || !b.getSchema().highPower) {
            return NetworkStrength.CopperCable;
        } else {
            IntMap<Boolean> connectionsA = highPowerConnections.get(a.getIndex());

            // might not actually be connectd
            if (connectionsA == null) return null;

            Boolean connectionA = connectionsA.get(b.getIndex());

            if (connectionA == null) {
                // we dont know about the connection: search it
                dfsScratch.clear();
                return findConnection(a, b);
            } else if (connectionA) {
                // we know of a fast connection
                return NetworkStrength.PowerPole;
            } else {
                // we know that no fast connection exists
                return NetworkStrength.CopperCable;
            }
        }
    }

    /**
     * Recursively search for a connection between a and b
     */
    private NetworkStrength findConnection(Structure<?> a, Structure<?> b) {
        int keyA = a.getIndex();
        int keyB = b.getIndex();

        dfsScratch.add(keyA);

        IntMap<Boolean> connectionsA = highPowerConnections.get(keyA);
        Boolean connectionA = connectionsA.get(keyB);

        if (connectionA == null) {
            // go through directly connected neighbors, 
            // maybe it's them or they already know about
            // the transitive connection

            // BFS first round for direct neighbors
            for (Edge e : edges.get(keyA)) {
                if (e != null && e.getNetworkStrength() == NetworkStrength.PowerPole) {
                    Structure<?> edgeB = e.getOther(a);

                    // do not go backwards in the path
                    if (dfsScratch.contains(edgeB.getIndex())) continue;

                    if (edgeB.getIndex() == keyB) {
                        // our direct neighbor is B
                        connectionA = true;
                        break;
                    }
                }
            }

            // go deeper if no direct neighbor
            if (connectionA == null) {
                for (Edge e : edges.get(keyA)) {
                    if (e != null && e.getNetworkStrength() == NetworkStrength.PowerPole) {
                        Structure<?> edgeB = e.getOther(a);

                        // do not go backwards in the path
                        if (dfsScratch.contains(edgeB.getIndex())) continue;

                        connectionA = findConnection(edgeB, b) == NetworkStrength.PowerPole;
                        if (connectionA != null) break;
                    }
                }
            }

            // if we could not find a path, there is none.
            if (connectionA == null) {
                connectionA = false;
            }

            // store information
            connectionsA.put(keyB, connectionA);
            IntMap<Boolean> connectionsB = highPowerConnections.get(keyB);
            connectionsB.put(keyA, connectionA);
        }

        return connectionA ? NetworkStrength.PowerPole : NetworkStrength.CopperCable;
    }

    public void clearHighPowerCache() {
        for (IntMap<Boolean> set : highPowerConnections.values())
            if (set != null) set.clear();
    }

    public boolean isEmpty() {
        return allVertices.isEmpty();
    }

    public boolean hasEdges() {
        return !edgeSet.isEmpty();
    }

    private void manageConnectedness() {
        // split off potential other network

        // gather connected component
        IntSet set = new IntSet();
        LinkedList<Integer> queue = new LinkedList<>();
        queue.add(edges.iterator().next().key);

        while (!queue.isEmpty()) {
            int n = queue.pop();
            Edge[] myEdges = edges.get(n);

            if (myEdges == null) continue; // what???
            set.add(n);

            for (Edge e : myEdges) {
                if (e == null) continue;

                if (e.getA().getIndex() == n && !set.contains(e.getB().getIndex())) {
                    queue.add(e.getB().getIndex());
                } else if (e.getB().getIndex() == n && !set.contains(e.getA().getIndex())) {
                    queue.add(e.getA().getIndex());
                }
            }
        }

        // disjunction, split off other part that is not connected to the found set
        if (set.size < edges.size) {
            PowerNetwork net = new PowerNetwork(grid);

            Set<Structure<?>> verticesToRemove = new HashSet<>();

            for (IntMap.Entry<Edge[]> o : edges.entries()) {
                // this vertex is not in the connected component
                if (!set.contains(o.key)) {
                    // migrate edges
                    for (Edge e : o.value) {
                        if (e == null) continue;

                        verticesToRemove.add(e.getA());
                        verticesToRemove.add(e.getB());
                        net.addEdge(e);
                    }
                }
            }

            // remove vertices from this network
            // separate call to not get any concurrent modification in the first loop
            for (Structure<?> s : verticesToRemove) {
                removeVertex(s, false);
            }

            grid.addNetwork(net);
        }
    }

    private void connect(Edge existingEdge, Structure<?> node, int nodeDockIndex,
            Structure<?> target, int targetDockIndex, NetworkStrength networkStrength) {
        if (existingEdge == null) {
            PowerNetwork oldNetwork = target.getPowerNetwork();

            addEdge(node, nodeDockIndex, target, targetDockIndex, networkStrength);
            grid.mergeNetworks(node.getPowerNetwork(), oldNetwork);
        } else if (existingEdge.getA() == node && target != existingEdge.getB()) {
            existingEdge.setB(target);
            existingEdge.setDockA(nodeDockIndex);
            existingEdge.setDockB(targetDockIndex);
            addVertex(target);
        } else if (existingEdge.getB() == node && target != existingEdge.getA()) {
            existingEdge.setA(target);
            existingEdge.setDockA(targetDockIndex);
            existingEdge.setDockB(nodeDockIndex);
            addVertex(target);
        }
    }

    public void updateConnections(Structure<?> node) {
        int key = node.getIndex();
        Edge[] myEdges = edges.get(key);

        for (int i = 0, j = 0; i < node.getDocks().length; i++) {
            Dock d = node.getDocks()[i];
            if (d.type == DockType.Power) {
                CopperCable c = node.layer.getCable(node.x + d.x + d.dir.dx, node.y + d.y + d.dir.dy);
                Edge e = myEdges[j];
                if (c != null) {
                    CopperCable endA = c.walkToEnd();
                    CopperCable endB = endA.walkToEnd();

                    if (endA == c || endB == c) {
                        // check if the cable we found next to the dock is actually terminating in the dock
                        if (c.getStructures()[d.dir.inv().ordinal()] == node) {
                            CopperCable other = null;
                            if (endA == c) {
                                other = endB;
                            } else {
                                other = endA;
                            }

                            boolean any = false;
                            for (Structure<?> s : other.getStructures()) {
                                if (s != null && !(s instanceof CopperCable) && s != node) {
                                    int k = 0;
                                    for (Dock d1 : s.getDocks()) {
                                        if (d1.type == DockType.Power) {
                                            if (s.x + d1.x + d1.dir.dx == other.x && s.y + d1.y + d1.dir.dy == other.y) {
                                                connect(e, node, j, s, k, NetworkStrength.CopperCable);
                                                any = true;
                                                break;
                                            }
                                        }
                                        if (d1.type == DockType.Power || d1.type == DockType.BigPower) k++;
                                    }

                                    if (any) break;
                                }
                            }

                            if (!any && e != null) {
                                removeEdge(e, true);
                            }
                        }
                    }
                } else if (e != null) {
                    removeEdge(e, true);
                }
                j++;
            } else if (d.type == DockType.BigPower) {
                Structure<?> ghost = node.layer.getStructure(node.x + d.x + d.dir.dx, node.y + d.y + d.dir.dy);

                Edge e = myEdges[j];

                Structure<?> other = null;
                int otherDock = -1;

                if (ghost instanceof PowerPoleGhost) {
                    PowerPole p = ((PowerPoleGhost) ghost).getPowerPole();
                    PowerPole end = p.walkToEnd();

                    if (end.getA() != null && !(end.getA() instanceof PowerPole) && end.getA() != node) {
                        other = end.getA();
                        otherDock = end.getAGhost().getDockIndex();
                    } else if (end.getB() != null && !(end.getB() instanceof PowerPole) && end.getB() != node) {
                        other = end.getB();
                        otherDock = end.getBGhost().getDockIndex();
                    }
                }

                if (other != null) {
                    connect(e, node, j, other, otherDock, NetworkStrength.PowerPole);
                } else if (e != null) {
                    removeEdge(e, true);
                }

                j++;
            }
        }
    }

    private boolean isArrayNull(Object[] o) {
        for (Object i : o)
            if (i != null) return false;
        return true;
    }

    private boolean removeEdge(Edge e, boolean createIslandNetworks) {
        int keyA = e.getA().getIndex();
        int keyB = e.getB().getIndex();
        Edge[] edgesOfA = edges.get(keyA);
        Edge[] edgesOfB = edges.get(keyB);

        if (edgesOfA == null || edgesOfB == null) return false;

        if (edgesOfA[e.getDockA()] == e && edgesOfB[e.getDockB()] == e) {
            edgeSet.remove(e);

            edgesOfA[e.getDockA()] = null;
            edges.put(keyA, edgesOfA);

            edgesOfB[e.getDockB()] = null;
            edges.put(keyB, edgesOfB);

            if (e.getNetworkStrength() == NetworkStrength.PowerPole) {
                IntMap<Boolean> connectionsA = highPowerConnections.get(keyA);
                connectionsA.remove(keyB);
                IntMap<Boolean> connectionsB = highPowerConnections.get(keyB);
                connectionsB.remove(keyA);

                grid.clearHighPowerCache();
            }

            if (createIslandNetworks) {
                // split up isolated nodes into separate network
                if (hasVertex(e.getA()) && isArrayNull(edgesOfA)) {
                    PowerNetwork n = new PowerNetwork(grid);
                    removeVertex(e.getA(), true);
                    n.addVertex(e.getA());
                    grid.addNetwork(n);
                }
                if (hasVertex(e.getB()) && isArrayNull(edgesOfB)) {
                    PowerNetwork n = new PowerNetwork(grid);
                    removeVertex(e.getB(), true);
                    n.addVertex(e.getB());
                    grid.addNetwork(n);
                }
            }

            hadEdgeDeletion = true;
            minimumSpanningTreeDirty = true;

            return true;
        }

        return false;
    }

    public float getMeanInPerSecond() {
        return inTickMean.getMean() / timeTickMean.getMean();
    }

    public float getTimeTickMean() {
        return timeTickMean.getMean();
    }

    public float getMeanOutPerSecond() {
        return outTickMean.getMean() / timeTickMean.getMean();
    }

    public void addOutTick(double amount) {
        outTick += amount;
    }

    public OrderedSet<Structure<?>> getAllVertices() {
        return allVertices;
    }

    private boolean isValidMSTEdge(Structure<?> from, Edge e, HashSet<Structure<?>> includedVertices) {
        if (e == null) return false;
        Structure<?> other = e.getOther(from);

        if (other == null || includedVertices.contains(other)) return false;
        // only allow cable shafts to go across layers
        if (other.layer.getIndex() != from.layer.getIndex()) return from.x == other.x && from.y == other.y;
        return true;
    }

    public Array<Edge> getMinimumSpanningTree() {
        if (minimumSpanningTreeDirty) {
            minimumSpanningTree.clear();

            if (allVertices.isEmpty()) {
                minimumSpanningTreeDirty = false;
                return minimumSpanningTree;
            }

            HashSet<Structure<?>> includedVertices = new HashSet<>();
            TreeSet<Edge> nextVertices = new TreeSet<>(new Comparator<Edge>() {
                @Override
                public int compare(Edge o1, Edge o2) {
                    int costA = Math.abs(o1.getB().getIndex() - o1.getA().getIndex());
                    int costB = Math.abs(o2.getB().getIndex() - o2.getA().getIndex());
                    if (costA - costB == 0) return o1.getA().getIndex() - o2.getA().getIndex();
                    return costA - costB;
                }
            });

            Structure<?> start = allVertices.first();
            includedVertices.add(start);
            Edge[] startEdges = edges.get(start.getIndex());
            if (startEdges != null) {
                for (Edge e : startEdges) {
                    if (isValidMSTEdge(start, e, includedVertices))
                        nextVertices.add(e);
                }
            }

            int allVerts = allVertices.size;
            while (includedVertices.size() < allVerts && !nextVertices.isEmpty()) {
                while (!nextVertices.isEmpty()) {
                    Edge e = nextVertices.pollFirst();
                    boolean hasA = includedVertices.contains(e.getA());
                    boolean hasB = includedVertices.contains(e.getB());
                    if (hasA && hasB) continue;

                    minimumSpanningTree.add(e);
                    Structure<?> newVert = e.getA();
                    if (hasA) newVert = e.getB();

                    includedVertices.add(newVert);
                    Edge[] nextEdges = edges.get(newVert.getIndex());
                    if (nextEdges != null) {
                        for (Edge edge : nextEdges) {
                            if (isValidMSTEdge(newVert, edge, includedVertices))
                                nextVertices.add(edge);
                        }
                    }
                }
            }

            minimumSpanningTreeDirty = false;
        }
        return minimumSpanningTree;
    }

    @Override
    public String toString() {
        String s = "{";

        for (Structure<?> p : donorVertices) {
            s += p.getIndex() + ", ";
        }
        if (s.length() > 1) s = s.substring(0, s.length() - 2);
        s += "; ";
        for (Structure<?> p : receiverVertices) {
            s += p.getIndex() + ", ";
        }
        if (s.length() > 1) s = s.substring(0, s.length() - 2);
        s += "; ";
        for (Structure<?> p : neutralVertices) {
            s += p.getIndex() + ", ";
        }
        if (s.length() > 1) s = s.substring(0, s.length() - 2);

        return s + "}";
    }
}
