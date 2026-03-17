---
name: costeo-motor-calculo
description: Patrones para motores de costeo y nutricion en CosteoApp — formulas, propagacion de precios, calculo de recetas, merma
version: 1.0.0
metadata:
  domain: business-logic
  triggers: motor, engine, costeo, precio, costo, nutricion, receta, prefabricado, calculo, merma, propagacion
  role: specialist
  scope: implementation
  output-format: code
---

# CosteoApp Motor de Calculo

Guia para implementar y extender los motores de logica de negocio (costeo, nutricion, precios).

## Arquitectura de Motores

Package: `com.mg.costeoapp.core.domain.engine`

Los motores son clases inyectadas con Hilt que encapsulan logica de negocio compleja. Reciben DAOs directamente (no Repositories) para eficiencia en calculos batch.

```
Engine (interface)
  └── EngineImpl (@Inject constructor, DAOs)
       ├── Funciones suspend para calculos puntuales
       └── Funciones Flow para calculos reactivos
```

## PricingEngine — Resolucion de Precios

Prioridad de resolucion:
1. **Inventario FIFO** — Lotes activos ordenados por fecha compra
2. **Precio reciente** — Ultimo precio registrado en productos_tiendas
3. **Sin precio** — Retornar warning, no bloquear calculo

```kotlin
data class PrecioResuelto(
    val precioUnitario: Double,
    val fuente: FuentePrecio,
    val tiendaNombre: String?,
    val fechaPrecio: Long?
)

enum class FuentePrecio {
    INVENTARIO_FIFO,
    PRECIO_REGISTRADO,
    SIN_PRECIO
}
```

## CosteoEngine — Calculo de Recetas/Prefabricados

Formula por ingrediente:
```
costoIngrediente = precioUnitario × cantidadUsada × (1 + porcentajeMerma/100)
```

Formula total prefabricado:
```
costoTotal = Σ(costoIngrediente) + costosFijos
costoPorPorcion = costoTotal / rendimientoPorciones
```

```kotlin
data class CosteoResult(
    val costoTotal: Double,
    val costoPorPorcion: Double,
    val detalleIngredientes: List<CosteoIngrediente>,
    val warnings: List<String>  // ingredientes sin precio, merma alta, etc.
)

data class CosteoIngrediente(
    val productoId: Long,
    val productoNombre: String,
    val cantidad: Double,
    val unidad: String,
    val precioUnitario: Double,
    val merma: Double,
    val costoConMerma: Double,
    val fuentePrecio: FuentePrecio
)
```

## NutricionEngine — Calculo Nutricional

Calcula macros/micros por porcion basado en datos nutricionales de ingredientes.

```kotlin
data class NutricionResult(
    val porPorcion: InfoNutricional,
    val porReceta: InfoNutricional,
    val completitud: Double,  // % de ingredientes con datos nutricionales
    val ingredientesSinDatos: List<String>
)

data class InfoNutricional(
    val calorias: Double,
    val proteinas: Double,
    val carbohidratos: Double,
    val grasas: Double,
    val fibra: Double,
    val sodio: Double
)
```

## Propagacion de Precios (Fase 4)

Cuando cambia un precio de producto, propagar a todos los prefabricados/platos que lo usan:

```
Precio producto cambia
  → Recalcular todos los prefabricados que usan ese producto
    → Recalcular todos los platos que usan esos prefabricados
      → Actualizar costos mostrados en UI (via Flow)
```

Implementar con Flow reactivo:
```kotlin
fun observeCostoPrefabricado(prefabricadoId: Long): Flow<CosteoResult> {
    return ingredienteDao.observeByPrefabricado(prefabricadoId)
        .flatMapLatest { ingredientes ->
            // Combinar flows de precios de cada ingrediente
            combine(ingredientes.map { observePrecio(it.productoId) }) { precios ->
                calculateCost(ingredientes, precios.toList())
            }
        }
}
```

## Conversion de Unidades

Tabla de conversiones comunes para El Salvador:
```kotlin
object UnidadConverter {
    fun convert(cantidad: Double, from: String, to: String): Double? {
        // kg ↔ lb (1 kg = 2.20462 lb)
        // lt ↔ ml (1 lt = 1000 ml)
        // unidad → no convertible
    }
}
```

## MUST DO
- Motores reciben DAOs via @Inject constructor
- Retornar data classes inmutables con resultados
- Incluir warnings (no excepciones) para datos incompletos
- Usar Double para valores monetarios y nutricionales
- Propagacion reactiva via Flow + combine/flatMapLatest
- Calculos en Dispatchers.Default para no bloquear IO
- Manejar division por cero (rendimiento = 0)

## MUST NOT DO
- Lanzar excepciones por datos faltantes (usar warnings)
- Usar Float para calculos (siempre Double)
- Acceder a UI desde motores
- Hardcodear precios o factores de conversion
- Ignorar merma en calculos de costo
- Bloquear main thread con calculos pesados

## Checklist
- [ ] Interface + Impl con @Inject
- [ ] Data classes para resultados (inmutables)
- [ ] Warnings para datos incompletos
- [ ] Tests unitarios para formulas
- [ ] Conversion de unidades cuando aplica
- [ ] Manejo de division por cero
- [ ] Flow reactivo para propagacion
