Lee TODAS las entidades Room (@Entity) del proyecto CosteoApp y genera un diagrama ER completo en formato Mermaid.

## Instrucciones

1. Busca todos los archivos en `core/database/entity/` que contengan `@Entity`
2. Para cada entidad, extrae:
   - Nombre de tabla (tableName)
   - Todos los campos con tipos
   - Primary keys
   - Foreign keys y relaciones
3. Genera un diagrama Mermaid `erDiagram` con:
   - Todas las entidades como bloques
   - Relaciones con cardinalidad correcta (||--o{, etc.)
   - Campos PK y FK marcados

## Formato de salida

Genera el diagrama en un archivo `docs/er-diagram.md` con el bloque Mermaid:

```mermaid
erDiagram
    TIENDAS {
        Long id PK
        String nombre
        Boolean activo
        ...
    }
    PRODUCTOS {
        Long id PK
        ...
    }
    TIENDAS ||--o{ PRODUCTO_TIENDA : tiene
    ...
```

## Reglas
- Incluir TODAS las entidades encontradas (no omitir ninguna)
- Marcar PK y FK explicitamente
- Usar nombres de tabla en MAYUSCULAS en el diagrama
- Agregar un resumen debajo del diagrama con: total de tablas, total de relaciones
