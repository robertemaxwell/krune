import { 
  Engine, 
  Scene, 
  ArcRotateCamera, 
  HemisphericLight, 
  MeshBuilder, 
  Vector3,
  Color3,
  StandardMaterial,
  Texture,
  DirectionalLight
} from "@babylonjs/core";

// Game client class to handle rendering and WebSocket communication
class GameClient {
  private canvas: HTMLCanvasElement;
  private engine: Engine;
  private scene: Scene;
  private socket: WebSocket | null = null;
  private playerMesh: any;
  private connected: boolean = false;
  private connectionStatusElement: HTMLElement | null = null;

  constructor(canvasElement: HTMLCanvasElement) {
    this.canvas = canvasElement;
    this.engine = new Engine(this.canvas, true);
    this.scene = new Scene(this.engine);
    
    // Get UI elements
    this.connectionStatusElement = document.getElementById('connection-status');
    
    // Initialize the 3D scene
    this.setupScene();
    
    // Handle window resize
    window.addEventListener("resize", () => {
      this.engine.resize();
    });
    
    // Start the render loop
    this.engine.runRenderLoop(() => {
      this.scene.render();
    });
    
    // Connect to the WebSocket server
    this.connectToServer();
  }

  // Set up the 3D scene with camera, lights, and basic environment
  private setupScene() {
    // Setup camera
    const camera = new ArcRotateCamera("camera", Math.PI / 2, Math.PI / 3, 20, new Vector3(0, 0, 0), this.scene);
    camera.lowerRadiusLimit = 10;
    camera.upperRadiusLimit = 50;
    camera.attachControl(this.canvas, true);
    
    // Setup lighting
    // Ambient light
    const hemisphericLight = new HemisphericLight("light1", new Vector3(0, 1, 0), this.scene);
    hemisphericLight.intensity = 0.7;
    
    // Directional light for shadows
    const directionalLight = new DirectionalLight("light2", new Vector3(0.5, -1, 1), this.scene);
    directionalLight.intensity = 0.5;
    
    // Create ground
    const ground = MeshBuilder.CreateGround("ground", { width: 50, height: 50 }, this.scene);
    
    // Add material to ground
    const groundMaterial = new StandardMaterial("groundMaterial", this.scene);
    groundMaterial.diffuseColor = new Color3(0.2, 0.6, 0.2); // Green color for grass
    ground.material = groundMaterial;
    
    // Create player character (placeholder)
    this.playerMesh = MeshBuilder.CreateBox("player", { width: 1, height: 2, depth: 1 }, this.scene);
    this.playerMesh.position = new Vector3(0, 1, 0);
    
    // Add material to player
    const playerMaterial = new StandardMaterial("playerMaterial", this.scene);
    playerMaterial.diffuseColor = new Color3(0.8, 0.4, 0.3);
    this.playerMesh.material = playerMaterial;
    
    // Add some environment objects for context
    this.createEnvironmentObjects();
  }
  
  // Create some basic environment objects to demonstrate scene rendering
  private createEnvironmentObjects() {
    // Create a few trees (simple cones and cylinders)
    for (let i = 0; i < 10; i++) {
      // Tree trunk (cylinder)
      const trunk = MeshBuilder.CreateCylinder("trunk" + i, { diameter: 0.6, height: 2 }, this.scene);
      
      // Random position
      const xPos = Math.random() * 40 - 20;
      const zPos = Math.random() * 40 - 20;
      trunk.position = new Vector3(xPos, 1, zPos);
      
      // Tree material
      const trunkMaterial = new StandardMaterial("trunkMaterial" + i, this.scene);
      trunkMaterial.diffuseColor = new Color3(0.4, 0.3, 0.2); // Brown
      trunk.material = trunkMaterial;
      
      // Tree leaves (using cylinder with tapered top instead of cone)
      const leaves = MeshBuilder.CreateCylinder("leaves" + i, { 
        diameterTop: 0.1,
        diameterBottom: 2.5,
        height: 3 
      }, this.scene);
      leaves.position = new Vector3(xPos, 3, zPos);
      
      // Leaves material
      const leavesMaterial = new StandardMaterial("leavesMaterial" + i, this.scene);
      leavesMaterial.diffuseColor = new Color3(0.1, 0.4, 0.1); // Dark green
      leaves.material = leavesMaterial;
    }
  }
  
  // Connect to the game server via WebSocket
  private connectToServer() {
    try {
      this.socket = new WebSocket("ws://localhost:8080/game");
      
      this.socket.onopen = () => {
        console.log("Connected to game server!");
        this.connected = true;
        this.updateConnectionStatus(true);
        if (this.socket) {
          this.socket.send("HELLO"); // Initial handshake
        }
      };
      
      this.socket.onmessage = (event) => {
        this.handleServerMessage(event.data);
      };
      
      this.socket.onerror = (error) => {
        console.error("WebSocket error:", error);
        this.connected = false;
        this.updateConnectionStatus(false);
      };
      
      this.socket.onclose = () => {
        console.log("Disconnected from server");
        this.connected = false;
        this.updateConnectionStatus(false);
        
        // Attempt to reconnect after 5 seconds
        setTimeout(() => {
          if (!this.connected) {
            console.log("Attempting to reconnect...");
            this.connectToServer();
          }
        }, 5000);
      };
    } catch (error) {
      console.error("Failed to connect to WebSocket server:", error);
      this.updateConnectionStatus(false);
    }
  }
  
  // Update the connection status UI
  private updateConnectionStatus(connected: boolean) {
    if (this.connectionStatusElement) {
      if (connected) {
        this.connectionStatusElement.className = 'connected';
        this.connectionStatusElement.textContent = 'Connected';
      } else {
        this.connectionStatusElement.className = 'disconnected';
        this.connectionStatusElement.textContent = 'Disconnected';
      }
    }
  }
  
  // Handle messages from the server
  private handleServerMessage(data: string) {
    console.log("Received from server:", data);
    
    if (data.startsWith("CONNECTED:")) {
      const sessionId = data.split(":")[1];
      console.log("Session established with ID:", sessionId);
      // You could store the session ID for later use
    }
    else if (data.startsWith("POSITION:")) {
      // Handle position updates for players/NPCs
      const parts = data.split(":");
      if (parts.length >= 4) {
        const x = parseFloat(parts[1]);
        const y = parseFloat(parts[2]);
        const z = parseFloat(parts[3]);
        
        // Update player position (for now just the local player)
        if (this.playerMesh) {
          this.playerMesh.position = new Vector3(x, y, z);
        }
      }
    }
  }
  
  // Send message to server
  public sendMessage(message: string) {
    if (this.connected && this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(message);
    } else {
      console.warn("Cannot send message, not connected to server");
    }
  }
  
  // Clean up resources when the client is destroyed
  public dispose() {
    this.engine.dispose();
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.close();
    }
  }
}

// Initialize the game client when the DOM is ready
window.addEventListener("DOMContentLoaded", () => {
  // Wait for the loading screen to finish
  let checkLoading = setInterval(() => {
    const loadingScreen = document.getElementById('loading-screen');
    if (loadingScreen && loadingScreen.style.display === 'none') {
      clearInterval(checkLoading);
      initGame();
    }
  }, 100);
  
  // Initialize the game after loading
  function initGame() {
    const canvasElement = document.getElementById("gameCanvas");
    if (!canvasElement || !(canvasElement instanceof HTMLCanvasElement)) {
      console.error("Canvas element not found or is not a canvas");
      return;
    }
    
    // Create and start the game client
    const client = new GameClient(canvasElement);
    
    // For debugging, make the client accessible globally
    (window as any).gameClient = client;
  }
}); 