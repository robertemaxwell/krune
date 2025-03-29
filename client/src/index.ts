import { Engine, Scene, ArcRotateCamera, HemisphericLight, MeshBuilder, Vector3 } from "@babylonjs/core";

window.addEventListener("DOMContentLoaded", () => {
  // Get the canvas element
  const canvasElement = document.getElementById("gameCanvas");
  if (!canvasElement || !(canvasElement instanceof HTMLCanvasElement)) {
    console.error("Canvas element not found or is not a canvas");
    return;
  }
  const canvas = canvasElement;
  
  // Create the Babylon.js engine
  const engine = new Engine(canvas, true);
  
  // Create a scene
  const scene = new Scene(engine);
  
  // Create a camera
  const camera = new ArcRotateCamera("camera", Math.PI / 2, Math.PI / 4, 10, Vector3.Zero(), scene);
  camera.attachControl(canvas, true);
  
  // Create a light
  const light = new HemisphericLight("light", new Vector3(0, 1, 0), scene);
  
  // Create a simple sphere (placeholder for character)
  const sphere = MeshBuilder.CreateSphere("sphere", { diameter: 2 }, scene);
  sphere.position.y = 1;
  
  // Create a ground
  MeshBuilder.CreateGround("ground", { width: 6, height: 6 }, scene);
  
  // Render loop
  engine.runRenderLoop(() => {
    scene.render();
  });
  
  // Handle window resize
  window.addEventListener("resize", () => {
    engine.resize();
  });
  
  // Connect to the WebSocket server
  const socket = new WebSocket("ws://localhost:8080/game");
  
  socket.onopen = () => {
    console.log("Connected to server!");
    socket.send("Hello from the client!");
  };
  
  socket.onmessage = (event) => {
    console.log("Received from server:", event.data);
  };
  
  socket.onerror = (error) => {
    console.error("WebSocket error:", error);
  };
  
  socket.onclose = () => {
    console.log("Disconnected from server");
  };
}); 