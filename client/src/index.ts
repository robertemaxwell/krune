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
  DirectionalLight,
  Animation
} from "@babylonjs/core";

// Interface for player state
interface PlayerState {
  id?: number;
  username?: string;
  x: number;
  y: number;
  z: number;
  direction: number;
  isRunning: boolean;
  health: number;
  skills: Map<number, { level: number, experience: number }>;
}

// Game client class to handle rendering and WebSocket communication
class GameClient {
  private canvas: HTMLCanvasElement;
  private engine: Engine;
  private scene: Scene;
  private socket: WebSocket | null = null;
  private playerMesh: any;
  private connected: boolean = false;
  private connectionStatusElement: HTMLElement | null = null;
  private loginFormElement: HTMLElement | null = null;
  private gameUIElement: HTMLElement | null = null;
  private chatLogElement: HTMLElement | null = null;
  
  // Player state
  private playerState: PlayerState = {
    x: 0,
    y: 0,
    z: 0,
    direction: 0,
    isRunning: false,
    health: 100,
    skills: new Map()
  };
  
  // Animation properties
  private movementSpeed: number = 0.15;
  private moveAnimation: Animation | null = null;

  constructor(canvasElement: HTMLCanvasElement) {
    this.canvas = canvasElement;
    this.engine = new Engine(this.canvas, true);
    this.scene = new Scene(this.engine);
    
    // Get UI elements
    this.connectionStatusElement = document.getElementById('connection-status');
    this.loginFormElement = document.getElementById('login-form');
    this.gameUIElement = document.getElementById('game-ui');
    this.chatLogElement = document.getElementById('chat-log');
    
    // Initialize the 3D scene
    this.setupScene();
    
    // Handle window resize
    window.addEventListener("resize", () => {
      this.engine.resize();
    });
    
    // Set up movement animation
    this.setupMovementAnimation();
    
    // Start the render loop
    this.engine.runRenderLoop(() => {
      this.scene.render();
    });
    
    // Connect to the WebSocket server
    this.connectToServer();
    
    // Set up UI event listeners
    this.setupUIEventListeners();
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
  
  // Set up movement animation
  private setupMovementAnimation() {
    // Create animation for player movement
    this.moveAnimation = new Animation(
      "moveAnimation", 
      "position", 
      30, 
      Animation.ANIMATIONTYPE_VECTOR3, 
      Animation.ANIMATIONLOOPMODE_CONSTANT
    );
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
  
  // Set up event listeners for UI elements
  private setupUIEventListeners() {
    // Handle login form submission
    const loginForm = document.getElementById('login-form') as HTMLFormElement;
    if (loginForm) {
      loginForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const username = (document.getElementById('username') as HTMLInputElement).value;
        const password = (document.getElementById('password') as HTMLInputElement).value;
        this.login(username, password);
      });
    }
    
    // Handle registration link
    const registerLink = document.getElementById('register-link');
    if (registerLink) {
      registerLink.addEventListener('click', (e) => {
        e.preventDefault();
        this.showRegistrationForm();
      });
    }
    
    // Handle registration form submission
    const registerForm = document.getElementById('register-form') as HTMLFormElement;
    if (registerForm) {
      registerForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const username = (document.getElementById('reg-username') as HTMLInputElement).value;
        const password = (document.getElementById('reg-password') as HTMLInputElement).value;
        this.register(username, password);
      });
    }
    
    // Handle login link from registration form
    const loginLink = document.getElementById('login-link');
    if (loginLink) {
      loginLink.addEventListener('click', (e) => {
        e.preventDefault();
        this.showLoginForm();
      });
    }
    
    // Handle run toggle button
    const runToggle = document.getElementById('run-toggle');
    if (runToggle) {
      runToggle.addEventListener('click', () => {
        this.toggleRun();
      });
    }
    
    // Handle click on the game canvas for movement
    this.canvas.addEventListener('click', (e) => {
      if (this.playerState.id) {  // Only allow movement if logged in
        // Convert click to world position (simplified)
        const x = Math.floor((e.offsetX / this.canvas.width) * 50 - 25);
        const z = Math.floor((e.offsetY / this.canvas.height) * 50 - 25);
        
        // Send movement command
        this.sendMessage(`MOVE:${x},${z}`);
      }
    });
  }
  
  // Show login form and hide registration form
  private showLoginForm() {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    
    if (loginForm && registerForm) {
      loginForm.style.display = 'block';
      registerForm.style.display = 'none';
    }
  }
  
  // Show registration form and hide login form
  private showRegistrationForm() {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    
    if (loginForm && registerForm) {
      loginForm.style.display = 'none';
      registerForm.style.display = 'block';
    }
  }
  
  // Attempt to login with provided credentials
  private login(username: string, password: string) {
    if (this.connected && this.socket) {
      this.socket.send(`LOGIN:${username}:${password}`);
    } else {
      this.addChatMessage('System', 'Not connected to server. Please try again later.', 'error');
    }
  }
  
  // Register a new account
  private register(username: string, password: string) {
    if (this.connected && this.socket) {
      this.socket.send(`REGISTER:${username}:${password}`);
    } else {
      this.addChatMessage('System', 'Not connected to server. Please try again later.', 'error');
    }
  }
  
  // Toggle run mode on/off
  private toggleRun() {
    if (this.connected && this.socket) {
      this.playerState.isRunning = !this.playerState.isRunning;
      this.socket.send(`RUN:${this.playerState.isRunning}`);
      
      // Update movement speed
      this.movementSpeed = this.playerState.isRunning ? 0.3 : 0.15;
      
      // Update UI
      const runToggle = document.getElementById('run-toggle');
      if (runToggle) {
        runToggle.textContent = this.playerState.isRunning ? 'Walk' : 'Run';
      }
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
  
  // Add a message to the chat log
  private addChatMessage(sender: string, message: string, type: 'system' | 'error' | 'chat' = 'system') {
    if (this.chatLogElement) {
      const messageElement = document.createElement('div');
      messageElement.className = `chat-message ${type}`;
      messageElement.innerHTML = `<span class="sender">${sender}:</span> ${message}`;
      this.chatLogElement.appendChild(messageElement);
      this.chatLogElement.scrollTop = this.chatLogElement.scrollHeight;
    }
  }
  
  // Show the game UI and hide login forms
  private showGameUI() {
    if (this.loginFormElement && this.gameUIElement) {
      this.loginFormElement.style.display = 'none';
      document.getElementById('register-form')!.style.display = 'none';
      this.gameUIElement.style.display = 'block';
    }
    
    // Update player name in UI
    const playerNameElement = document.getElementById('player-name');
    if (playerNameElement && this.playerState.username) {
      playerNameElement.textContent = this.playerState.username;
    }
  }
  
  // Handle messages from the server
  private handleServerMessage(data: string) {
    console.log("Received from server:", data);
    
    if (data.startsWith("CONNECTED:")) {
      const sessionId = data.split(":")[1];
      console.log("Session established with ID:", sessionId);
      this.addChatMessage('System', 'Connected to server. Please login or register.');
    }
    else if (data.startsWith("LOGIN_SUCCESS:")) {
      const parts = data.split(":");
      if (parts.length >= 3) {
        this.playerState.id = parseInt(parts[1]);
        this.playerState.username = parts[2];
        this.addChatMessage('System', `Welcome back, ${this.playerState.username}!`);
        this.showGameUI();
      }
    }
    else if (data.startsWith("LOGIN_FAILED:")) {
      const reason = data.split(":")[1] || "Unknown error";
      this.addChatMessage('System', `Login failed: ${reason}`, 'error');
    }
    else if (data.startsWith("REGISTER_SUCCESS:")) {
      const parts = data.split(":");
      if (parts.length >= 3) {
        this.playerState.id = parseInt(parts[1]);
        this.playerState.username = parts[2];
        this.addChatMessage('System', `Welcome to the game, ${this.playerState.username}!`);
        this.showGameUI();
      }
    }
    else if (data.startsWith("REGISTER_FAILED:")) {
      const reason = data.split(":")[1] || "Unknown error";
      this.addChatMessage('System', `Registration failed: ${reason}`, 'error');
    }
    else if (data.startsWith("POSITION:")) {
      const parts = data.split(":");
      if (parts.length >= 5) {
        const x = parseInt(parts[1]);
        const y = parseInt(parts[2]);
        const z = parseInt(parts[3]);
        const direction = parseInt(parts[4]);
        
        // Store in player state
        this.playerState.x = x;
        this.playerState.y = y;
        this.playerState.z = z;
        this.playerState.direction = direction;
        
        // Update player mesh position with animation
        this.animatePlayerMovement(x, y, z);
        
        // Rotate player based on direction
        const rotation = (direction * Math.PI / 4);  // 8 directions (0-7)
        this.playerMesh.rotation.y = rotation;
      }
    }
    else if (data.startsWith("RUN_TOGGLE:")) {
      const isRunning = data.split(":")[1] === "true";
      this.playerState.isRunning = isRunning;
      this.movementSpeed = isRunning ? 0.3 : 0.15;
      
      // Update UI
      const runToggle = document.getElementById('run-toggle');
      if (runToggle) {
        runToggle.textContent = isRunning ? 'Walk' : 'Run';
      }
    }
    else if (data.startsWith("LOGOUT_SUCCESS")) {
      this.playerState = {
        x: 0,
        y: 0,
        z: 0,
        direction: 0,
        isRunning: false,
        health: 100,
        skills: new Map()
      };
      
      if (this.loginFormElement && this.gameUIElement) {
        this.loginFormElement.style.display = 'block';
        this.gameUIElement.style.display = 'none';
      }
      
      this.addChatMessage('System', 'You have been logged out.');
    }
  }
  
  // Animate player movement to new position
  private animatePlayerMovement(x: number, y: number, z: number) {
    // Convert game coordinates to scene coordinates
    const targetPosition = new Vector3(x, y + 1, z);  // +1 for height offset
    
    if (this.moveAnimation && this.playerMesh) {
      // Stop any running animations
      this.scene.stopAnimation(this.playerMesh);
      
      // Create animation keyframes
      const keyFrames = [
        { frame: 0, value: this.playerMesh.position.clone() },
        { frame: 60, value: targetPosition }
      ];
      
      this.moveAnimation.setKeys(keyFrames);
      
      // Start animation
      this.scene.beginDirectAnimation(
        this.playerMesh,
        [this.moveAnimation],
        0,
        60,
        false,
        this.playerState.isRunning ? 2 : 1 // Speed multiplier
      );
    } else {
      // Fallback if animation not available
      if (this.playerMesh) {
        this.playerMesh.position = targetPosition;
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
  
  // Logout the current player
  public logout() {
    if (this.connected && this.socket && this.playerState.id) {
      this.socket.send("LOGOUT");
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
    
    // Set up logout button
    const logoutButton = document.getElementById('logout-button');
    if (logoutButton) {
      logoutButton.addEventListener('click', () => {
        client.logout();
      });
    }
    
    // For debugging, make the client accessible globally
    (window as any).gameClient = client;
  }
}); 