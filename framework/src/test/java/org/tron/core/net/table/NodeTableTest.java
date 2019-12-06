package org.tron.core.net.table;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.tron.common.overlay.discover.node.Node;
import org.tron.common.overlay.discover.table.KademliaOptions;
import org.tron.common.overlay.discover.table.NodeEntry;
import org.tron.common.overlay.discover.table.NodeTable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class NodeTableTest implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger("Test");
    private Node homeNode;
    private NodeTable nodeTable;
    private String[] ips;
    private List<byte[]> ids;

    @Override
    public void run() {

    }

    @Before
    public void init() {
        ids = new ArrayList();
        for (int i = 0; i < KademliaOptions.BUCKET_SIZE + 1; i++) {
            byte[] id = new byte[64];
            id[0] = 17;
            id[1] = 16;
            if (i < 10) {
                id[63] = (byte)i;
            } else {
                id[62] = 1;
                id[63] = (byte)(i - 10);
            }
            ids.add(id);
        }

        ips = new String[KademliaOptions.BUCKET_SIZE + 1];
        byte[] homeId = new byte[64];
        homeNode = new Node(homeId, "127.0.0.1", 18888, 18888);
        nodeTable = new NodeTable(homeNode);
        ips[0] = "127.0.0.2";
        ips[1] = "127.0.0.3";
        ips[2] = "127.0.0.4";
        ips[3] = "127.0.0.5";
        ips[4] = "127.0.0.6";
        ips[5] = "127.0.0.7";
        ips[6] = "127.0.0.8";
        ips[7] = "127.0.0.9";
        ips[8] = "127.0.0.10";
        ips[9] = "127.0.0.11";
        ips[10] = "127.0.0.12";
        ips[11] = "127.0.0.13";
        ips[12] = "127.0.0.14";
        ips[13] = "127.0.0.15";
        ips[14] = "127.0.0.16";
        ips[15] = "127.0.0.17";
        ips[16] = "127.0.0.18";
    }

    @Test
    public void addNodeTest() {
        Node node = new Node(ids.get(0), ips[0], 18888, 18888);
        Assert.assertEquals(0, nodeTable.getNodesCount());
        nodeTable.addNode(node);
        Assert.assertEquals(1, nodeTable.getNodesCount());
        Assert.assertTrue(nodeTable.contains(node));
    }

    @Test
    public void addDupNodeTest() {
        Node node = new Node(ids.get(0), ips[0], 18888, 18888);
        nodeTable.addNode(node);
        long firstTouchTime = nodeTable.getAllNodes().get(0).getModified();
        nodeTable.addNode(node);
        Assert.assertEquals(1, nodeTable.getNodesCount());
        long lastTouchTime = nodeTable.getAllNodes().get(0).getModified();
        Assert.assertTrue(lastTouchTime > firstTouchTime);
    }

    @Test
    public void addNode_bucketFullTest() throws Exception{
        for (int i = 0; i < KademliaOptions.BUCKET_SIZE; i++) {
            TimeUnit.MILLISECONDS.sleep(10);
            addNode(new Node(ids.get(i),ips[i],18888,18888));
        }
        Node lastSeen = nodeTable.addNode(new Node(ids.get(16),ips[16],18888,18888));
        Assert.assertTrue(null != lastSeen);
        Assert.assertEquals(ips[15],lastSeen.getHost());

    }

    public void addNode(Node n) {
        nodeTable.addNode(n);
    }

    @Test
    public void dropNodeTest() {
        Node node = new Node(ids.get(0), ips[0], 18888, 18888);
        nodeTable.addNode(node);
        Assert.assertTrue(nodeTable.contains(node));
        nodeTable.dropNode(node);
        Assert.assertTrue(!nodeTable.contains(node));
    }

    @Test
    public void getBucketsCountTest() {
        Assert.assertEquals(0,nodeTable.getBucketsCount());
        Node node = new Node(ids.get(0), ips[0], 18888, 18888);
        nodeTable.addNode(node);
        Assert.assertEquals(1,nodeTable.getBucketsCount());
    }

    @Test
    public void touchNodeTest() throws Exception{
        Node node = new Node(ids.get(0), ips[0], 18888, 18888);
        nodeTable.addNode(node);
        long firstTouchTime = nodeTable.getAllNodes().get(0).getModified();
        TimeUnit.MILLISECONDS.sleep(10);
        nodeTable.touchNode(node);
        long lastTouchTime = nodeTable.getAllNodes().get(0).getModified();
        Assert.assertTrue(firstTouchTime < lastTouchTime);
    }

    @Test
    public void containsTest() {
        Node node = new Node(ids.get(0), ips[0], 18888, 18888);
        Assert.assertTrue(!nodeTable.contains(node));
        nodeTable.addNode(node);
        Assert.assertTrue(nodeTable.contains(node));
    }

    @Test
    public void getBuckIdTest() {
        Node node = new Node(ids.get(0), ips[0], 18888, 18888);  //id: 11100...000
        nodeTable.addNode(node);
        NodeEntry nodeEntry = new NodeEntry(homeNode.getId(),node);
        Assert.assertEquals(252, nodeTable.getBucketId(nodeEntry));
    }

    @Test
    public void getClosestNodes_NodesMoreThanBucketCapacity() throws Exception{
        byte[] bytes = new byte[64];
        bytes[0] = 15;
        Node nearNode = new Node(bytes, "127.0.0.19", 18888,18888);
        bytes[0] = 70;
        Node farNode = new Node(bytes, "127.0.0.20", 18888,18888);
        nodeTable.addNode(nearNode);
        nodeTable.addNode(farNode);
        for (int i = 0; i < KademliaOptions.BUCKET_SIZE - 1; i++) {  //To control totally 17 nodes, however closest's capacity is 16
            nodeTable.addNode(new Node(ids.get(i), ips[i], 18888, 18888));
            TimeUnit.MILLISECONDS.sleep(10);
        }
        Assert.assertEquals(3,nodeTable.getBucketsCount());  //3 buckets, nearnode's distance is 252, far's is 255, others' are 253

        List<Node> closest = nodeTable.getClosestNodes(homeNode.getId());
        Assert.assertTrue(closest.contains(nearNode));
        Assert.assertTrue(!closest.contains(farNode));    //the farest node should be excluded
    }

    @Test
    public void getClosestNodes_isDiscoverNode() {
        Node node = new Node(ids.get(0), ips[0], 18888); //This constructor builds a node with isFakeNodeId = true
        nodeTable.addNode(node);
        List<Node> closest = nodeTable.getClosestNodes(homeNode.getId());
        Assert.assertTrue(closest.isEmpty());
    }
}
