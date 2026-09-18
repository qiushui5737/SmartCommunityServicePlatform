<template>
  <div class="building-3d-wrapper">
    <div ref="canvasRef" class="canvas-3d"></div>
    <!-- 悬浮提示 -->
    <div v-if="tooltip.show" class="tooltip-3d"
         :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
      <div class="tt-room">{{ tooltip.roomNo }}</div>
      <div class="tt-status">
        <span class="tt-dot" :style="{ background: tooltip.color }"></span>
        {{ tooltip.statusText }}
      </div>
      <div class="tt-area">{{ tooltip.area }} ㎡</div>
    </div>
    <!-- 操作提示 -->
    <div class="controls-hint">
      🖱️ 拖拽旋转 &nbsp; 🔍 滚轮缩放 &nbsp; 👆 点击查看详情
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'

const props = defineProps({
  houses: { type: Array, default: () => [] },
  unitList: { type: Array, default: () => [] },
  floorList: { type: Array, default: () => [] }
})

const emit = defineEmits(['room-click'])

const canvasRef = ref(null)
const tooltip = ref({ show: false, x: 0, y: 0, roomNo: '', statusText: '', color: '', area: '' })

let scene, camera, renderer, controls, raycaster, mouse
let roomMeshes = []
let hoveredMesh = null
let animationId = null

const statusColors = {
  OCCUPIED: 0x22c55e,
  VACANT: 0x94a3b8
}
const statusTexts = {
  OCCUPIED: '已入住',
  VACANT: '空置'
}
const statusHoverColors = {
  OCCUPIED: 0x16a34a,
  VACANT: 0x64748b
}

const initScene = () => {
  if (!canvasRef.value) return
  const w = canvasRef.value.clientWidth
  const h = canvasRef.value.clientHeight

  // Scene
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xf0f4f8)

  // Camera
  camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 1000)
  camera.position.set(25, 22, 25)
  camera.lookAt(0, 8, 0)

  // Renderer
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  canvasRef.value.appendChild(renderer.domElement)

  // Lights
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)

  const dirLight = new THREE.DirectionalLight(0xffffff, 0.8)
  dirLight.position.set(20, 30, 20)
  dirLight.castShadow = true
  dirLight.shadow.mapSize.width = 2048
  dirLight.shadow.mapSize.height = 2048
  scene.add(dirLight)

  const fillLight = new THREE.DirectionalLight(0xb4d4ff, 0.3)
  fillLight.position.set(-15, 10, -10)
  scene.add(fillLight)

  // Ground
  const groundGeo = new THREE.PlaneGeometry(60, 60)
  const groundMat = new THREE.MeshStandardMaterial({ color: 0xe2e8f0, roughness: 0.9 })
  const ground = new THREE.Mesh(groundGeo, groundMat)
  ground.rotation.x = -Math.PI / 2
  ground.position.y = -0.1
  ground.receiveShadow = true
  scene.add(ground)

  // Grid helper
  const grid = new THREE.GridHelper(60, 30, 0xcbd5e1, 0xe2e8f0)
  grid.position.y = 0
  scene.add(grid)

  // Controls
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.minDistance = 8
  controls.maxDistance = 60
  controls.maxPolarAngle = Math.PI / 2 - 0.05
  controls.target.set(0, 8, 0)

  // Raycaster
  raycaster = new THREE.Raycaster()
  mouse = new THREE.Vector2()

  // Events
  renderer.domElement.addEventListener('mousemove', onMouseMove)
  renderer.domElement.addEventListener('click', onClick)
  window.addEventListener('resize', onResize)

  animate()
}

const buildRooms = () => {
  // Remove old meshes
  roomMeshes.forEach(m => scene.remove(m))
  roomMeshes = []

  if (!props.houses.length || !props.unitList.length || !props.floorList.length) return

  const unitList = props.unitList
  const floorList = props.floorList
  const floorCount = floorList.length

  // Room dimensions
  const rW = 1.4   // room width
  const rH = 1.05  // room height (floor)
  const rD = 1.2   // room depth
  const gap = 0.06  // gap between rooms
  const corrW = 0.6 // corridor width
  const wingGap = 0.3 // gap between wing and center

  // Cross layout: 4 wings from center
  // Wing E (+X): rooms face outward along +X
  // Wing W (-X): rooms face outward along -X
  // Wing N (-Z): rooms face outward along -Z
  // Wing S (+Z): rooms face outward along +Z

  // Group units into wings (distribute units across 4 wings)
  const wingNames = ['E', 'W', 'N', 'S']
  const unitToWing = {}
  unitList.forEach((u, i) => { unitToWing[u] = wingNames[i % 4] })

  // For each wing, count max rooms per floor
  const wingRooms = {}
  wingNames.forEach(w => {
    const wingUnits = unitList.filter(u => unitToWing[u] === w)
    let maxRooms = 0
    floorList.forEach(f => {
      const cnt = wingUnits.reduce((sum, u) => {
        return sum + props.houses.filter(h => h.floorNum === f && h.unitNo === u).length
      }, 0)
      maxRooms = Math.max(maxRooms, cnt)
    })
    wingRooms[w] = maxRooms
  })

  // Calculate wing extents (how long each wing is)
  const wingLen = {}
  wingNames.forEach(w => {
    wingLen[w] = wingRooms[w] * (rW + gap) - gap
  })

  // Helper: create a room mesh + label
  const createRoom = (house, x, y, z, labelFace) => {
    const geo = new THREE.BoxGeometry(rW, rH, rD)
    const color = statusColors[house.status] || 0x94a3b8
    const mat = new THREE.MeshStandardMaterial({
      color, roughness: 0.4, metalness: 0.1,
      transparent: true, opacity: 0.92
    })
    const mesh = new THREE.Mesh(geo, mat)
    mesh.position.set(x, y, z)
    mesh.castShadow = true
    mesh.receiveShadow = true
    mesh.userData = { ...house, originalColor: color }
    scene.add(mesh)
    roomMeshes.push(mesh)

    // Room number label on the outward face
    const canvas = document.createElement('canvas')
    canvas.width = 96; canvas.height = 48
    const ctx = canvas.getContext('2d')
    ctx.font = 'bold 22px Arial'
    ctx.fillStyle = '#ffffff'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(house.roomNo, 48, 24)
    const texture = new THREE.CanvasTexture(canvas)
    const spriteMat = new THREE.SpriteMaterial({ map: texture, transparent: true })
    const sprite = new THREE.Sprite(spriteMat)
    const off = rW / 2 + 0.05
    const offZ = rD / 2 + 0.05
    if (labelFace === 'E') sprite.position.set(x + off, y, z)
    else if (labelFace === 'W') sprite.position.set(x - off, y, z)
    else if (labelFace === 'S') sprite.position.set(x, y, z + offZ)
    else sprite.position.set(x, y, z - offZ)
    sprite.scale.set(0.8, 0.4, 1)
    scene.add(sprite)
    roomMeshes.push(sprite)
  }

  // Build rooms for each floor
  floorList.forEach((floor, fi) => {
    const y = (floorCount - 1 - fi) * (rH + gap) + rH / 2 + 0.2

    wingNames.forEach(wing => {
      const wingUnits = unitList.filter(u => unitToWing[u] === wing)
      // Collect all rooms for this wing on this floor
      const allRooms = []
      wingUnits.forEach(u => {
        props.houses
          .filter(h => h.floorNum === floor && h.unitNo === u)
          .sort((a, b) => a.roomNo.localeCompare(b.roomNo))
          .forEach(h => allRooms.push(h))
      })

      allRooms.forEach((house, ri) => {
        let x = 0, z = 0
        const idx = ri  // position index in the wing
        const offset = wingGap + corrW / 2 + rW / 2 + idx * (rW + gap)

        if (wing === 'E') {
          // Wing extends to +X, two rows along Z
          x = offset
          z = (ri % 2 === 0) ? (rD / 2 + 0.02) : -(rD / 2 + 0.02)
          createRoom(house, x, y, z, 'E')
        } else if (wing === 'W') {
          // Wing extends to -X
          x = -offset
          z = (ri % 2 === 0) ? (rD / 2 + 0.02) : -(rD / 2 + 0.02)
          createRoom(house, x, y, z, 'W')
        } else if (wing === 'S') {
          // Wing extends to +Z
          z = offset
          x = (ri % 2 === 0) ? (rW / 2 + 0.02) : -(rW / 2 + 0.02)
          createRoom(house, x, y, z, 'S')
        } else {
          // Wing N extends to -Z
          z = -offset
          x = (ri % 2 === 0) ? (rW / 2 + 0.02) : -(rW / 2 + 0.02)
          createRoom(house, x, y, z, 'N')
        }
      })
    })
  })

  // Central core (elevator/stairwell shaft)
  const coreH = floorCount * (rH + gap) + 0.4
  const coreGeo = new THREE.BoxGeometry(corrW + 0.4, coreH, corrW + 0.4)
  const coreMat = new THREE.MeshStandardMaterial({ color: 0x334155, roughness: 0.6 })
  const coreMesh = new THREE.Mesh(coreGeo, coreMat)
  coreMesh.position.set(0, coreH / 2, 0)
  coreMesh.castShadow = true
  scene.add(coreMesh)

  // Corridors on each floor (cross-shaped)
  floorList.forEach((floor, fi) => {
    const y = (floorCount - 1 - fi) * (rH + gap) + rH / 2 + 0.2
    const maxLen = Math.max(wingLen.E, wingLen.W, wingLen.N, wingLen.S) + wingGap * 2

    // East corridor
    const eLen = wingLen['E'] + wingGap + corrW / 2
    if (eLen > 0) {
      const cGeo = new THREE.BoxGeometry(eLen, 0.08, corrW)
      const cMat = new THREE.MeshStandardMaterial({ color: 0x64748b, roughness: 0.8 })
      const c = new THREE.Mesh(cGeo, cMat)
      c.position.set(eLen / 2, y - rH / 2, 0)
      scene.add(c); roomMeshes.push(c)
    }
    // West corridor
    const wLen = wingLen['W'] + wingGap + corrW / 2
    if (wLen > 0) {
      const cGeo = new THREE.BoxGeometry(wLen, 0.08, corrW)
      const cMat = new THREE.MeshStandardMaterial({ color: 0x64748b, roughness: 0.8 })
      const c = new THREE.Mesh(cGeo, cMat)
      c.position.set(-wLen / 2, y - rH / 2, 0)
      scene.add(c); roomMeshes.push(c)
    }
    // South corridor
    const sLen = wingLen['S'] + wingGap + corrW / 2
    if (sLen > 0) {
      const cGeo = new THREE.BoxGeometry(corrW, 0.08, sLen)
      const cMat = new THREE.MeshStandardMaterial({ color: 0x64748b, roughness: 0.8 })
      const c = new THREE.Mesh(cGeo, cMat)
      c.position.set(0, y - rH / 2, sLen / 2)
      scene.add(c); roomMeshes.push(c)
    }
    // North corridor
    const nLen = wingLen['N'] + wingGap + corrW / 2
    if (nLen > 0) {
      const cGeo = new THREE.BoxGeometry(corrW, 0.08, nLen)
      const cMat = new THREE.MeshStandardMaterial({ color: 0x64748b, roughness: 0.8 })
      const c = new THREE.Mesh(cGeo, cMat)
      c.position.set(0, y - rH / 2, -nLen / 2)
      scene.add(c); roomMeshes.push(c)
    }
  })

  // Cross-shaped base
  const maxExtE = wingLen['E'] + wingGap + rW + 1
  const maxExtW = wingLen['W'] + wingGap + rW + 1
  const maxExtS = wingLen['S'] + wingGap + rD + 1
  const maxExtN = wingLen['N'] + wingGap + rD + 1
  // E-W bar
  const barW = maxExtE + maxExtW
  const barGeo = new THREE.BoxGeometry(barW, 0.3, corrW + rD * 2 + 1)
  const barMat = new THREE.MeshStandardMaterial({ color: 0x475569, roughness: 0.7 })
  const bar = new THREE.Mesh(barGeo, barMat)
  bar.position.set((maxExtE - maxExtW) / 2, -0.15, 0)
  bar.receiveShadow = true; scene.add(bar)
  // N-S bar
  const barD = maxExtN + maxExtS
  const barGeo2 = new THREE.BoxGeometry(corrW + rW * 2 + 1, 0.3, barD)
  const bar2 = new THREE.Mesh(barGeo2, barMat)
  bar2.position.set(0, -0.15, (maxExtS - maxExtN) / 2)
  bar2.receiveShadow = true; scene.add(bar2)

  // Floor labels on the side
  floorList.forEach((floor, fi) => {
    const canvas = document.createElement('canvas')
    canvas.width = 64; canvas.height = 64
    const ctx = canvas.getContext('2d')
    ctx.font = 'bold 32px Arial'
    ctx.fillStyle = '#64748b'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(floor + 'F', 32, 32)
    const texture = new THREE.CanvasTexture(canvas)
    const spriteMat = new THREE.SpriteMaterial({ map: texture, transparent: true })
    const sprite = new THREE.Sprite(spriteMat)
    const yPos = (floorCount - 1 - fi) * (rH + gap) + rH / 2 + 0.2
    sprite.position.set(-maxExtW - 1.5, yPos, 0)
    sprite.scale.set(0.8, 0.8, 1)
    scene.add(sprite); roomMeshes.push(sprite)
  })

  // Update camera target
  controls.target.set(0, floorCount * (rH + gap) / 2, 0)
}

const onMouseMove = (event) => {
  if (!renderer) return
  const rect = renderer.domElement.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(mouse, camera)
  const intersects = raycaster.intersectObjects(roomMeshes.filter(m => m.isMesh))

  // Reset previous hover
  if (hoveredMesh && hoveredMesh.material) {
    hoveredMesh.material.color.setHex(hoveredMesh.userData.originalColor)
    hoveredMesh.material.emissive?.setHex(0x000000)
  }

  if (intersects.length > 0) {
    const mesh = intersects[0].object
    if (mesh.userData.roomNo) {
      hoveredMesh = mesh
      mesh.material.emissive?.setHex(0x222222)

      tooltip.value = {
        show: true,
        x: event.clientX - canvasRef.value.getBoundingClientRect().left + 15,
        y: event.clientY - canvasRef.value.getBoundingClientRect().top - 10,
        roomNo: mesh.userData.roomNo,
        statusText: statusTexts[mesh.userData.status] || mesh.userData.status,
        color: '#' + mesh.userData.originalColor.toString(16).padStart(6, '0'),
        area: mesh.userData.area
      }
      renderer.domElement.style.cursor = 'pointer'
    }
  } else {
    hoveredMesh = null
    tooltip.value.show = false
    renderer.domElement.style.cursor = 'default'
  }
}

const onClick = (event) => {
  if (!renderer) return
  const rect = renderer.domElement.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(mouse, camera)
  const intersects = raycaster.intersectObjects(roomMeshes.filter(m => m.isMesh))

  if (intersects.length > 0 && intersects[0].object.userData.roomNo) {
    emit('room-click', intersects[0].object.userData)
  }
}

const onResize = () => {
  if (!canvasRef.value || !renderer || !camera) return
  const w = canvasRef.value.clientWidth
  const h = canvasRef.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

const animate = () => {
  animationId = requestAnimationFrame(animate)
  controls?.update()
  renderer?.render(scene, camera)
}

const cleanup = () => {
  cancelAnimationFrame(animationId)
  renderer?.domElement.removeEventListener('mousemove', onMouseMove)
  renderer?.domElement.removeEventListener('click', onClick)
  window.removeEventListener('resize', onResize)
  roomMeshes.forEach(m => {
    if (m.geometry) m.geometry.dispose()
    if (m.material) {
      if (m.material.map) m.material.map.dispose()
      m.material.dispose()
    }
    scene?.remove(m)
  })
  renderer?.dispose()
  controls?.dispose()
}

onMounted(async () => {
  await nextTick()
  initScene()
  buildRooms()
})

watch(() => [props.houses, props.unitList, props.floorList], () => {
  buildRooms()
}, { deep: true })

onBeforeUnmount(cleanup)
</script>

<style scoped>
.building-3d-wrapper {
  position: relative;
  width: 100%;
  height: 550px;
  border-radius: 12px;
  overflow: hidden;
}

.canvas-3d {
  width: 100%;
  height: 100%;
}

.tooltip-3d {
  position: absolute;
  background: rgba(15, 23, 42, 0.9);
  color: white;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  pointer-events: none;
  z-index: 10;
  backdrop-filter: blur(8px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  min-width: 100px;

  .tt-room { font-weight: 700; font-size: 16px; margin-bottom: 4px; }
  .tt-status { display: flex; align-items: center; gap: 6px; margin-bottom: 2px; }
  .tt-dot { width: 8px; height: 8px; border-radius: 50%; }
  .tt-area { color: rgba(255,255,255,0.7); font-size: 12px; }
}

.controls-hint {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(15, 23, 42, 0.7);
  color: rgba(255,255,255,0.8);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  backdrop-filter: blur(8px);
}
</style>
