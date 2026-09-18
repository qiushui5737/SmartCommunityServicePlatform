<template>
  <div class="parking-3d-wrapper">
    <div ref="canvasRef" class="canvas-3d"></div>
    <!-- 悬浮提示 -->
    <div v-if="tooltip.show" class="tooltip-3d"
         :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
      <div class="tt-no">{{ tooltip.spaceNo }}</div>
      <div class="tt-row">
        <span class="tt-dot" :style="{ background: tooltip.color }"></span>
        {{ tooltip.statusText }}
      </div>
      <div class="tt-owner" v-if="tooltip.ownerName">业主：{{ tooltip.ownerName }}</div>
      <div class="tt-info">{{ tooltip.typeText }} · {{ tooltip.area }}㎡</div>
      <div class="tt-price" v-if="tooltip.price">¥{{ tooltip.price }}</div>
    </div>
    <!-- 操作提示 -->
    <div class="controls-hint">
      🖱️ 拖拽旋转 &nbsp; 🔍 滚轮缩放 &nbsp; 👆 点击车位
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'

const props = defineProps({
  zones: { type: Array, default: () => [] },   // grid zone data
  selectedZone: { type: String, default: '' },
  statusColors: {
    type: Object,
    default: () => ({
      FREE: 0x52c41a, SOLD: 0xbfbfbf, LOCKED: 0xff4d4f, RESERVED: 0xfaad14
    })
  },
  statusTexts: {
    type: Object,
    default: () => ({
      FREE: '空闲', SOLD: '已售', LOCKED: '锁定', RESERVED: '预订'
    })
  },
  typeTexts: {
    type: Object,
    default: () => ({
      STANDARD: '标准', COMPACT: '微型', LARGE: '大型', VIP: 'VIP'
    })
  }
})

const emit = defineEmits(['space-click'])

const canvasRef = ref(null)
const tooltip = ref({ show: false, x: 0, y: 0, spaceNo: '', statusText: '', color: '', typeText: '', area: '', price: '', ownerName: '' })

let scene, camera, renderer, controls, raycaster, mouse
let spaceMeshes = []
let hoveredMesh = null
let animationId = null

const initScene = () => {
  if (!canvasRef.value) return
  const w = canvasRef.value.clientWidth
  const h = canvasRef.value.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xeef2f7)

  camera = new THREE.PerspectiveCamera(40, w / h, 0.1, 500)
  camera.position.set(25, 30, 25)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  canvasRef.value.appendChild(renderer.domElement)

  // Lights
  scene.add(new THREE.AmbientLight(0xffffff, 0.65))
  const dir = new THREE.DirectionalLight(0xffffff, 0.8)
  dir.position.set(20, 35, 15)
  dir.castShadow = true
  dir.shadow.mapSize.width = 2048
  dir.shadow.mapSize.height = 2048
  scene.add(dir)
  const fill = new THREE.DirectionalLight(0xb4d4ff, 0.3)
  fill.position.set(-15, 10, -10)
  scene.add(fill)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.minDistance = 8
  controls.maxDistance = 80
  controls.maxPolarAngle = Math.PI / 2 - 0.05

  raycaster = new THREE.Raycaster()
  mouse = new THREE.Vector2()

  renderer.domElement.addEventListener('mousemove', onMouseMove)
  renderer.domElement.addEventListener('click', onClick)
  window.addEventListener('resize', onResize)

  animate()
}

const buildParking = () => {
  // Clean up
  spaceMeshes.forEach(m => scene.remove(m))
  spaceMeshes = []

  const zone = props.zones.find(z => z.zone === props.selectedZone)
  if (!zone || !zone.rows) return

  const rows = zone.rows
  const maxRow = zone.maxRow || rows.length
  const maxCol = zone.maxCol || (rows[0] ? rows[0].length : 0)

  // Dimensions
  const spaceW = 2.4   // space width (along X)
  const spaceD = 4.5   // space depth (along Z, like a real parking spot)
  const spaceH = 0.12  // thin slab
  const gapX = 0.15    // gap between spaces
  const laneW = 3.0    // driving lane width between row pairs
  const rowPairGap = laneW + spaceD  // distance between two facing rows

  // Parking lots are arranged as:
  // Row 1 spots → lane → Row 2 spots → lane → Row 3 ...
  // Spaces face the lane (depth along Z, arranged along X)

  const totalW = maxCol * (spaceW + gapX) - gapX
  const totalD = maxRow * (spaceD + 0.8) // some gap between rows for lanes
  const offsetX = -totalW / 2
  const offsetZ = -totalD / 2

  // Ground plane
  const groundGeo = new THREE.PlaneGeometry(totalW + 10, totalD + 10)
  const groundMat = new THREE.MeshStandardMaterial({ color: 0x3a3a3a, roughness: 0.95 })
  const ground = new THREE.Mesh(groundGeo, groundMat)
  ground.rotation.x = -Math.PI / 2
  ground.position.y = -0.05
  ground.receiveShadow = true
  scene.add(ground)
  spaceMeshes.push(ground)

  // Lane markings (white stripes between rows)
  for (let r = 0; r < maxRow; r++) {
    const z = offsetZ + r * (spaceD + 0.8) + spaceD + 0.4
    // Lane stripe
    const stripeGeo = new THREE.PlaneGeometry(totalW + 4, 0.15)
    const stripeMat = new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.5 })
    const stripe = new THREE.Mesh(stripeGeo, stripeMat)
    stripe.rotation.x = -Math.PI / 2
    stripe.position.set(0, 0.01, z)
    scene.add(stripe)
    spaceMeshes.push(stripe)
  }

  // Dashed center line in each lane
  for (let r = 0; r < maxRow - 1; r++) {
    const z = offsetZ + (r + 1) * (spaceD + 0.8) - 0.4
    for (let d = -totalW / 2; d < totalW / 2; d += 1.5) {
      const dashGeo = new THREE.PlaneGeometry(0.8, 0.08)
      const dashMat = new THREE.MeshStandardMaterial({ color: 0xfbbf24 })
      const dash = new THREE.Mesh(dashGeo, dashMat)
      dash.rotation.x = -Math.PI / 2
      dash.position.set(d + 0.4, 0.01, z)
      scene.add(dash)
      spaceMeshes.push(dash)
    }
  }

  // Build each parking space
  rows.forEach((row, rIdx) => {
    row.forEach((cell, cIdx) => {
      if (!cell) return // skip empty (lane) cells

      const color = props.statusColors[cell.status] || 0x999999
      const geo = new THREE.BoxGeometry(spaceW, spaceH, spaceD)
      const mat = new THREE.MeshStandardMaterial({
        color,
        roughness: 0.5,
        metalness: 0.05,
        transparent: true,
        opacity: 0.92
      })
      const mesh = new THREE.Mesh(geo, mat)

      const x = offsetX + cIdx * (spaceW + gapX) + spaceW / 2
      const y = spaceH / 2
      const z = offsetZ + rIdx * (spaceD + 0.8) + spaceD / 2

      mesh.position.set(x, y, z)
      mesh.castShadow = true
      mesh.receiveShadow = true
      mesh.userData = {
        ...cell,
        originalColor: color,
        rowIdx: rIdx,
        colIdx: cIdx
      }
      scene.add(mesh)
      spaceMeshes.push(mesh)

      // White parking lines around each space
      const lineMat = new THREE.MeshStandardMaterial({ color: 0xffffff })
      // Left line
      const lGeo = new THREE.BoxGeometry(0.06, 0.01, spaceD)
      const leftLine = new THREE.Mesh(lGeo, lineMat)
      leftLine.position.set(x - spaceW / 2, 0.02, z)
      scene.add(leftLine); spaceMeshes.push(leftLine)
      // Right line
      const rightLine = new THREE.Mesh(lGeo, lineMat)
      rightLine.position.set(x + spaceW / 2, 0.02, z)
      scene.add(rightLine); spaceMeshes.push(rightLine)
      // Back line
      const bGeo = new THREE.BoxGeometry(spaceW, 0.01, 0.06)
      const backLine = new THREE.Mesh(bGeo, lineMat)
      backLine.position.set(x, 0.02, z - spaceD / 2)
      scene.add(backLine); spaceMeshes.push(backLine)

      // Space number label (sprite on top)
      const canvas = document.createElement('canvas')
      canvas.width = 128; canvas.height = 64
      const ctx = canvas.getContext('2d')
      ctx.font = 'bold 26px Arial'
      ctx.fillStyle = '#ffffff'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(cell.spaceNo || '', 64, 32)
      const tex = new THREE.CanvasTexture(canvas)
      const spriteMat = new THREE.SpriteMaterial({ map: tex, transparent: true })
      const sprite = new THREE.Sprite(spriteMat)
      sprite.position.set(x, 0.35, z)
      sprite.scale.set(1.8, 0.9, 1)
      scene.add(sprite)
      spaceMeshes.push(sprite)
    })
  })

  // Row labels on the side
  for (let r = 0; r < maxRow; r++) {
    const canvas = document.createElement('canvas')
    canvas.width = 64; canvas.height = 64
    const ctx = canvas.getContext('2d')
    ctx.font = 'bold 28px Arial'
    ctx.fillStyle = '#94a3b8'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(`${r + 1}排`, 32, 32)
    const tex = new THREE.CanvasTexture(canvas)
    const sMat = new THREE.SpriteMaterial({ map: tex, transparent: true })
    const s = new THREE.Sprite(sMat)
    const z = offsetZ + r * (spaceD + 0.8) + spaceD / 2
    s.position.set(offsetX - 2.5, 1, z)
    s.scale.set(1.5, 1.5, 1)
    scene.add(s); spaceMeshes.push(s)
  }

  // Adjust camera
  const maxDim = Math.max(totalW, totalD)
  controls.target.set(0, 0, 0)
  camera.position.set(maxDim * 0.6, maxDim * 0.7, maxDim * 0.6)
}

const onMouseMove = (event) => {
  if (!renderer) return
  const rect = renderer.domElement.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(mouse, camera)
  const meshes = spaceMeshes.filter(m => m.isMesh && m.userData.spaceNo)
  const intersects = raycaster.intersectObjects(meshes)

  if (hoveredMesh && hoveredMesh.material && hoveredMesh.material.emissive) {
    hoveredMesh.material.emissive.setHex(0x000000)
  }

  if (intersects.length > 0) {
    const mesh = intersects[0].object
    if (mesh.userData.spaceNo) {
      hoveredMesh = mesh
      if (mesh.material.emissive) mesh.material.emissive.setHex(0x222222)

      const c = '#' + mesh.userData.originalColor.toString(16).padStart(6, '0')
      tooltip.value = {
        show: true,
        x: event.clientX - canvasRef.value.getBoundingClientRect().left + 15,
        y: event.clientY - canvasRef.value.getBoundingClientRect().top - 10,
        spaceNo: mesh.userData.spaceNo,
        statusText: props.statusTexts[mesh.userData.status] || mesh.userData.status,
        color: c,
        typeText: props.typeTexts[mesh.userData.type] || mesh.userData.type || '',
        area: mesh.userData.area || '-',
        price: mesh.userData.price ? Number(mesh.userData.price).toFixed(0) : '',
        ownerName: mesh.userData.ownerName || ''
      }
      renderer.domElement.style.cursor = mesh.userData.status === 'FREE' ? 'pointer' : 'default'
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
  const meshes = spaceMeshes.filter(m => m.isMesh && m.userData.spaceNo)
  const intersects = raycaster.intersectObjects(meshes)

  if (intersects.length > 0 && intersects[0].object.userData.spaceNo) {
    emit('space-click', intersects[0].object.userData)
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
  spaceMeshes.forEach(m => {
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
  buildParking()
})

watch(() => [props.zones, props.selectedZone], () => {
  buildParking()
}, { deep: true })

onBeforeUnmount(cleanup)
</script>

<style scoped>
.parking-3d-wrapper {
  position: relative;
  width: 100%;
  height: 520px;
  border-radius: 12px;
  overflow: hidden;
  background: #1a1a2e;
}

.canvas-3d {
  width: 100%;
  height: 100%;
}

.tooltip-3d {
  position: absolute;
  background: rgba(15, 23, 42, 0.92);
  color: white;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  pointer-events: none;
  z-index: 10;
  backdrop-filter: blur(8px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  min-width: 120px;

  .tt-no { font-weight: 700; font-size: 16px; margin-bottom: 4px; }
  .tt-row { display: flex; align-items: center; gap: 6px; margin-bottom: 2px; }
  .tt-dot { width: 8px; height: 8px; border-radius: 50%; }
  .tt-owner { color: #60a5fa; font-size: 12px; margin-bottom: 2px; }
  .tt-info { color: rgba(255,255,255,0.7); font-size: 12px; margin-bottom: 2px; }
  .tt-price { color: #f59e0b; font-weight: 600; font-size: 14px; }
}

.controls-hint {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(15, 23, 42, 0.7);
  color: rgba(255,255,255,0.8);
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  backdrop-filter: blur(8px);
}
</style>
