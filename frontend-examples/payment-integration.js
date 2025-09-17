// =====================================
// EJEMPLOS DE USO - FRONTEND INTEGRATION
// Sistema de Pagos con Mercado Pago
// =====================================

// 1. CREAR PREFERENCIA DE PAGO
// =============================

/**
 * Función para crear una preferencia de pago en Mercado Pago
 */
const createPaymentPreference = async (membershipPlanId, userToken) => {
  try {
    const response = await fetch('http://localhost:8081/api/payments/create-preference', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userToken}`
      },
      body: JSON.stringify({
        membershipPlanId: membershipPlanId,
        description: `Membresía del gimnasio - Plan ${membershipPlanId}`,
        payerEmail: 'cliente@example.com' // Opcional, se toma del usuario autenticado
      })
    });

    const data = await response.json();

    if (data.success) {
      console.log('Preferencia creada exitosamente:', data);
      
      // Redirigir al usuario a Mercado Pago
      window.location.href = data.initPoint; // Producción
      // window.location.href = data.sandboxInitPoint; // Para testing
      
      return data;
    } else {
      throw new Error(data.message);
    }
  } catch (error) {
    console.error('Error creando preferencia de pago:', error);
    throw error;
  }
};

// 2. OBTENER INFORMACIÓN DE MEMBRESÍA
// ===================================

/**
 * Función para obtener información detallada de la membresía del usuario
 */
const getMembershipInfo = async (userToken) => {
  try {
    const response = await fetch('http://localhost:8081/api/membership-info-enhanced', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${userToken}`
      }
    });

    if (response.ok) {
      const membershipInfo = await response.json();
      console.log('Información de membresía:', membershipInfo);
      return membershipInfo;
    } else {
      throw new Error('Error obteniendo información de membresía');
    }
  } catch (error) {
    console.error('Error:', error);
    throw error;
  }
};

// 3. OBTENER HISTORIAL DE PAGOS
// =============================

/**
 * Función para obtener el historial de pagos del usuario
 */
const getMyPayments = async (userToken) => {
  try {
    const response = await fetch('http://localhost:8081/api/payments/my-payments', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${userToken}`
      }
    });

    if (response.ok) {
      const payments = await response.json();
      console.log('Historial de pagos:', payments);
      return payments;
    } else {
      throw new Error('Error obteniendo historial de pagos');
    }
  } catch (error) {
    console.error('Error:', error);
    throw error;
  }
};

// 4. COMPONENTE REACT - EJEMPLO DE IMPLEMENTACIÓN
// ===============================================

import React, { useState, useEffect } from 'react';

const PaymentComponent = ({ userToken, membershipPlans }) => {
  const [membershipInfo, setMembershipInfo] = useState(null);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadMembershipInfo();
    loadPayments();
  }, []);

  const loadMembershipInfo = async () => {
    try {
      const info = await getMembershipInfo(userToken);
      setMembershipInfo(info);
    } catch (error) {
      console.error('Error cargando información de membresía:', error);
    }
  };

  const loadPayments = async () => {
    try {
      const paymentHistory = await getMyPayments(userToken);
      setPayments(paymentHistory);
    } catch (error) {
      console.error('Error cargando pagos:', error);
    }
  };

  const handlePayment = async (planId) => {
    setLoading(true);
    try {
      await createPaymentPreference(planId, userToken);
      // El usuario será redirigido a Mercado Pago
    } catch (error) {
      alert('Error al crear el pago: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP'
    }).format(amount);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('es-CL');
  };

  return (
    <div className="payment-component">
      {/* Información de Membresía Actual */}
      <div className="membership-info">
        <h2>Mi Membresía</h2>
        {membershipInfo ? (
          <div className={`membership-card ${membershipInfo.active ? 'active' : 'inactive'}`}>
            <h3>{membershipInfo.membershipPlan}</h3>
            <p>Estado: {membershipInfo.active ? 'Activa' : 'Inactiva'}</p>
            {membershipInfo.active && (
              <>
                <p>Días restantes: {membershipInfo.daysRemaining}</p>
                <p>Vence: {formatDate(membershipInfo.endDate)}</p>
              </>
            )}
            <p>Total de pagos: {membershipInfo.totalPayments}</p>
          </div>
        ) : (
          <p>Cargando información de membresía...</p>
        )}
      </div>

      {/* Planes de Membresía Disponibles */}
      <div className="membership-plans">
        <h2>Planes Disponibles</h2>
        <div className="plans-grid">
          {membershipPlans.map(plan => (
            <div key={plan.id} className="plan-card">
              <h3>{plan.name}</h3>
              <p className="price">{formatCurrency(plan.price)}</p>
              <p>{plan.durationMonths} {plan.durationMonths === 1 ? 'mes' : 'meses'}</p>
              <p>{plan.daysPerWeek} días por semana</p>
              <button
                onClick={() => handlePayment(plan.id)}
                disabled={loading}
                className="pay-button"
              >
                {loading ? 'Procesando...' : 'Comprar'}
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Historial de Pagos */}
      <div className="payment-history">
        <h2>Historial de Pagos</h2>
        {payments.length > 0 ? (
          <div className="payments-list">
            {payments.map(payment => (
              <div key={payment.id} className={`payment-item ${payment.status.toLowerCase()}`}>
                <div className="payment-info">
                  <h4>{payment.membershipPlanName}</h4>
                  <p>{payment.description}</p>
                  <p>Fecha: {formatDate(payment.createdAt)}</p>
                </div>
                <div className="payment-details">
                  <p className="amount">{formatCurrency(payment.amount)}</p>
                  <p className={`status ${payment.status.toLowerCase()}`}>
                    {getStatusText(payment.status)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p>No hay pagos registrados</p>
        )}
      </div>
    </div>
  );
};

// Función auxiliar para traducir estados
const getStatusText = (status) => {
  const statusMap = {
    'PENDING': 'Pendiente',
    'APPROVED': 'Aprobado',
    'REJECTED': 'Rechazado',
    'CANCELLED': 'Cancelado',
    'IN_PROCESS': 'En Proceso',
    'REFUNDED': 'Reembolsado'
  };
  return statusMap[status] || status;
};

export default PaymentComponent;

// 5. CSS SUGERIDO
// ===============

const styles = `
.payment-component {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.membership-info {
  margin-bottom: 30px;
}

.membership-card {
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 20px;
  background: #f9f9f9;
}

.membership-card.active {
  border-color: #4caf50;
  background: #e8f5e8;
}

.membership-card.inactive {
  border-color: #f44336;
  background: #ffeaea;
}

.plans-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin: 20px 0;
}

.plan-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.price {
  font-size: 1.5em;
  font-weight: bold;
  color: #2196f3;
  margin: 10px 0;
}

.pay-button {
  background: #2196f3;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  margin-top: 15px;
}

.pay-button:hover {
  background: #1976d2;
}

.pay-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.payments-list {
  space-y: 10px;
}

.payment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 6px;
  margin-bottom: 10px;
}

.payment-item.approved {
  border-color: #4caf50;
  background: #f1f8e9;
}

.payment-item.pending {
  border-color: #ff9800;
  background: #fff3e0;
}

.payment-item.rejected {
  border-color: #f44336;
  background: #ffebee;
}

.status {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.875em;
  font-weight: bold;
}

.status.approved {
  background: #4caf50;
  color: white;
}

.status.pending {
  background: #ff9800;
  color: white;
}

.status.rejected {
  background: #f44336;
  color: white;
}
`;

// 6. MANEJO DE URLS DE RETORNO
// =============================

// Ejemplo para manejar las URLs de retorno de Mercado Pago
// En tu router (React Router, Next.js, etc.)

// Página de éxito
const PaymentSuccessPage = () => {
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const paymentId = urlParams.get('payment_id');
    const status = urlParams.get('status');
    const externalReference = urlParams.get('external_reference');

    console.log('Pago exitoso:', { paymentId, status, externalReference });

    // Actualizar el estado de la aplicación
    // Redirigir al dashboard del usuario
    setTimeout(() => {
      window.location.href = '/dashboard';
    }, 3000);
  }, []);

  return (
    <div className="payment-success">
      <h1>¡Pago Exitoso!</h1>
      <p>Tu membresía ha sido activada correctamente.</p>
      <p>Serás redirigido al dashboard en unos segundos...</p>
    </div>
  );
};

// Página de fallo
const PaymentFailurePage = () => {
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const paymentId = urlParams.get('payment_id');
    const status = urlParams.get('status');

    console.log('Pago fallido:', { paymentId, status });
  }, []);

  return (
    <div className="payment-failure">
      <h1>Pago No Procesado</h1>
      <p>Hubo un problema con tu pago. Por favor, intenta nuevamente.</p>
      <button onClick={() => window.location.href = '/memberships'}>
        Intentar Nuevamente
      </button>
    </div>
  );
};

// 7. TESTING CON POSTMAN O CURL
// =============================

/*
// 1. Autenticarse primero
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "password": "password123"
  }'

// 2. Crear preferencia de pago (usar el token del paso anterior)
curl -X POST http://localhost:8081/api/payments/create-preference \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "membershipPlanId": 1,
    "description": "Membresía Premium - Test",
    "payerEmail": "test@example.com"
  }'

// 3. Obtener información de membresía
curl -X GET http://localhost:8081/api/membership-info-enhanced \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

// 4. Obtener historial de pagos
curl -X GET http://localhost:8081/api/payments/my-payments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
*/
