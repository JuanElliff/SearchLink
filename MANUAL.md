# Manual de usuario — SearchLink

SearchLink es una plataforma para difundir y colaborar en la búsqueda de personas desaparecidas.
Cuando se publica una alerta, los ciudadanos cercanos reciben un aviso y pueden reportar si vieron a
la persona. Hay tres tipos de usuario:

- **Ciudadano** (estándar): ve las alertas, reporta avistamientos y recibe notificaciones.
- **Operador** (fuerza de seguridad): crea y gestiona las alertas, y revisa los avistamientos.
- **Administrador**: gestiona las cuentas de usuario de la plataforma.

Según tu tipo de usuario vas a ver una pantalla y unas acciones distintas. Este manual está
organizado por rol.

---

## Cómo entrar

### Registrarse (solo ciudadanos)

Si sos un ciudadano y todavía no tenés cuenta, podés crearte una vos mismo:

1. En la pantalla de ingreso, tocá **"Registrate"**.
2. En la pantalla **"Crear cuenta"**, completá:
   - **Nombre**
   - **Email**
   - **Contraseña** (mínimo 8 caracteres)
   - **Ubicación**: marcala tocando el punto en el mapa, o usá el botón **"Usar mi ubicación"** para
     que el navegador la detecte automáticamente.
3. Tocá **"Crear cuenta"**.

Al registrarte quedás como **usuario estándar (ciudadanía)**.

> Los **operadores** y **administradores** no se registran solos: sus cuentas las crea un
> administrador.

### Iniciar sesión

1. En la pantalla **"SearchLink" / "Ingresá a tu cuenta"**, ingresá tu **Email** y **Contraseña**.
2. Tocá **"Ingresar"**.

Según tu tipo de usuario vas a entrar a una pantalla inicial distinta. Si el email o la contraseña
no son correctos, aparece el mensaje **"Email o contraseña incorrectos"**.

### Activar notificaciones (para todos)

En la barra superior, en cualquier pantalla, hay un botón **"Activar notificaciones"**:

1. Tocá **"Activar notificaciones"**. El navegador te va a pedir permiso para enviarte avisos.
2. Una vez aceptado, el botón se reemplaza por **"✓ Notificaciones activas"** (en verde): ya están
   funcionando.

Si en algún momento ves **"Notif. bloqueadas"** (en amarillo), significa que el navegador tiene los
avisos bloqueados. Para reactivarlos: hacé clic en el ícono de candado (o de información) en la barra
del navegador → **Permisos del sitio** → **Notificaciones** → **Permitir**, y recargá la página.

Con la aplicación abierta, los avisos llegan como un **cartelito arriba a la derecha** (tocalo para
cerrarlo; se cierra solo a los pocos segundos).

---

## Usuario ciudadano (estándar)

Tu pantalla inicial es **"Alertas activas"**.

### Ver las alertas

1. La pantalla **"Alertas activas"** muestra un **mapa** con un **marcador por cada alerta** vigente.
2. Tocá un marcador para ver el nombre y la edad, y luego **"Ver detalle"**.

Si no hay alertas, vas a ver **"No hay alertas activas en este momento."**

### Ver el detalle de una alerta

La pantalla de detalle muestra:

- **Nombre** y **edad** de la persona.
- El **estado**: **"Activa"**, **"Resuelta"** o **"Cancelada"**.
- **Foto** (si la alerta tiene).
- **Descripción**.
- Un **mapa** con la ubicación y el **radio de búsqueda**.

Si la alerta está **Activa**, aparece el botón **"Reportar avistamiento"**. Para volver, usá
**"Volver al mapa"**.

### Reportar un avistamiento

Si viste a la persona de una alerta activa:

1. En el detalle de la alerta, tocá **"Reportar avistamiento"**.
2. Completá:
   - **Descripción**: dónde y cuándo la viste, qué ropa llevaba.
   - **Ubicación donde lo viste**: marcala en el mapa (o usá **"Usar mi ubicación"**).
   - **Foto** (opcional): podés elegir una imagen y, en el celular, **sacar una foto en el momento**.
3. Tocá **"Enviar avistamiento"**.

Al enviarlo verás la confirmación **"✓ Avistamiento reportado"** y **"Gracias por tu colaboración."**
No hace falta que cargues tus datos: tu identidad se toma automáticamente de tu sesión.

### Recibir alertas

Con las notificaciones activas (ver más arriba), vas a recibir un **aviso automático** cuando se
publique una alerta **cerca tuyo**.

---

## Usuario operador (fuerza de seguridad)

Tu pantalla inicial es **"Alertas activas"**.

### Ver y administrar alertas

La pantalla muestra la **lista de alertas activas**. Arriba está el botón **"+ Crear alerta"**, y por
cada alerta tenés los botones **"Editar"** y **"Moderar"**.

> Solo se muestran las alertas activas. Las resueltas o canceladas se acceden por su enlace directo.

### Crear una alerta

1. Tocá **"+ Crear alerta"**.
2. En la pantalla **"Nueva alerta"**, completá:
   - **Nombre del menor**
   - **Edad** (opcional)
   - **Descripción** (por ejemplo, la ropa que llevaba o señas particulares)
   - **Foto** (opcional)
   - **Ubicación**: marcala en el mapa.
   - **Radio de búsqueda (km)**: hasta qué distancia se difunde la alerta.
3. Tocá **"Crear alerta"**.

Al crearla, se **notifica automáticamente** a los ciudadanos que están dentro del radio indicado.

### Editar una alerta

1. En la lista, tocá **"Editar"** en la alerta correspondiente.
2. Podés cambiar **solo** dos cosas:
   - **Estado**: **Activa**, **Resuelta** o **Cancelada**.
   - **Radio de búsqueda (km)**.
3. Tocá **"Guardar cambios"** (aparece la confirmación **"✓ Cambios guardados"**).

### Moderar avistamientos

1. En la lista, tocá **"Moderar"** en una alerta.
2. Vas a ver un **mapa** con la alerta y los **avistamientos reportados**, y debajo la **lista** de
   esos avistamientos. Cada uno trae su descripción, fecha, foto (si tiene) y un estado:
   **Pendiente**, **Verificado** o **Descartado**.
3. Para cada avistamiento **Pendiente** podés escribir un **comentario opcional** y luego decidir:
   - **"Verificar"**: lo confirmás como válido.
   - **"Descartar"**: lo marcás como no relevante.

---

## Usuario administrador

Tu pantalla inicial es **"Gestión de usuarios"**.

### Gestionar usuarios

La pantalla muestra la **lista de usuarios** con su **nombre**, **email**, **rol** y estado
(**Activo** / **Inactivo**). Arriba está el botón **"+ Crear usuario"**, y por cada usuario hay un
botón **"Desactivar"** o **"Activar"** según su estado.

> No podés desactivarte a vos mismo: ese botón aparece deshabilitado en tu propia cuenta.

### Crear un usuario

1. Tocá **"+ Crear usuario"**.
2. En la pantalla **"Crear usuario"**, completá:
   - **Rol**: **Operador** o **Administrador**.
   - **Nombre**
   - **Email**
   - **Contraseña** (mínimo 8 caracteres)
   - **Ubicación**: marcala en el mapa.
3. Tocá **"Crear usuario"**. Si todo está bien, aparece la confirmación **"✓ [nombre] creado como
   [rol]"**.

> El administrador **no crea ciudadanos** (esos se registran solos) y **no crea ni modera alertas**
> (eso es tarea del operador).

---

## Referencia rápida — qué hace cada rol

| Rol | Qué puede hacer |
|---|---|
| **Ciudadano (estándar)** | Ve las alertas, reporta avistamientos y recibe notificaciones. |
| **Operador** | Crea y edita alertas, y modera los avistamientos. |
| **Administrador** | Gestiona las cuentas: crea operadores/administradores y los activa o desactiva. |
