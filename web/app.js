// Fuel Tracker - Progressive Web App Logic
(function () {
  'use strict';

  // Storage key
  const STORAGE_KEY = 'fuel_tracker_pwa_records';

  // State
  let state = {
    amount: 1000,
    price: 64.25,
    odometer: 325662,
    efficiency: 15.0,
    date: new Date().toISOString().split('T')[0],
    notes: '',
    records: []
  };

  // DOM Elements
  const headerDateEl = document.getElementById('header-date');
  const odoDisplayEl = document.getElementById('current-odometer-display');
  const statTotalSpentEl = document.getElementById('stat-total-spent');
  const statAvgEfficiencyEl = document.getElementById('stat-avg-efficiency');
  const statAvgSubEl = document.getElementById('stat-avg-sub');
  
  const inputDateEl = document.getElementById('input-date');
  const inputAmountEl = document.getElementById('input-amount');
  const inputPriceEl = document.getElementById('input-price');
  const inputOdometerEl = document.getElementById('input-odometer');
  const inputEfficiencyEl = document.getElementById('input-efficiency');
  const sliderEfficiencyEl = document.getElementById('slider-efficiency');
  const displayEfficiencyEl = document.getElementById('display-efficiency');
  const inputNotesEl = document.getElementById('input-notes');
  const priceRateBadgeEl = document.getElementById('price-rate-badge');

  const forecastCardEl = document.getElementById('forecast-card');
  const forecastLitresEl = document.getElementById('forecast-litres');
  const forecastRangeEl = document.getElementById('forecast-range');
  const forecastTargetOdoEl = document.getElementById('forecast-target-odo');
  const forecastTargetDescEl = document.getElementById('forecast-target-desc');
  
  const btnSaveEl = document.getElementById('btn-save');
  const historyListEl = document.getElementById('history-list');
  const emptyHistoryEl = document.getElementById('empty-history');
  const historyBadgeCountEl = document.getElementById('history-badge-count');
  const toastBannerEl = document.getElementById('toast-banner');
  const toastMessageEl = document.getElementById('toast-message');

  const btnExportEl = document.getElementById('btn-export');
  const fileImportEl = document.getElementById('file-import');
  const btnInstallPwaEl = document.getElementById('btn-install-pwa');

  let deferredInstallPrompt = null;

  // Initialize
  function init() {
    loadRecords();
    initHeaderDate();
    initFormDefaults();
    bindEvents();
    render();
    registerServiceWorker();
  }

  function initHeaderDate() {
    const options = { weekday: 'long', month: 'short', day: '2-digit' };
    headerDateEl.textContent = new Intl.DateTimeFormat('en-US', options).format(new Date());
  }

  function initFormDefaults() {
    inputDateEl.value = state.date;
    inputAmountEl.value = state.amount;
    inputPriceEl.value = state.price;
    inputOdometerEl.value = state.odometer;
    inputEfficiencyEl.value = state.efficiency.toFixed(1);
    sliderEfficiencyEl.value = state.efficiency;
    displayEfficiencyEl.textContent = `${state.efficiency.toFixed(1)} km/L`;
    priceRateBadgeEl.textContent = `Rs ${state.price} / L`;

    // If records exist, update current odometer from latest record
    if (state.records.length > 0) {
      const latest = state.records[0]; // sorted newest first
      state.odometer = latest.targetOdometer || latest.odometerReading;
      inputOdometerEl.value = state.odometer;
      if (latest.pricePerLitre) {
        state.price = latest.pricePerLitre;
        inputPriceEl.value = state.price;
        priceRateBadgeEl.textContent = `Rs ${state.price} / L`;
      }
      if (latest.averageMileage) {
        state.efficiency = latest.averageMileage;
        sliderEfficiencyEl.value = state.efficiency;
        inputEfficiencyEl.value = state.efficiency.toFixed(1);
        displayEfficiencyEl.textContent = `${state.efficiency.toFixed(1)} km/L`;
      }
    }
  }

  function loadRecords() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        state.records = JSON.parse(saved);
      }
    } catch (e) {
      console.error('Failed to load records from localStorage', e);
      state.records = [];
    }
  }

  function saveRecordsToStorage() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state.records));
    } catch (e) {
      console.error('Failed to save records to localStorage', e);
    }
  }

  function calculateForecast() {
    const amount = parseFloat(inputAmountEl.value) || 0;
    const price = parseFloat(inputPriceEl.value) || 0;
    const odometer = parseFloat(inputOdometerEl.value) || 0;
    const efficiency = parseFloat(sliderEfficiencyEl.value) || 15.0;

    if (amount > 0 && price > 0) {
      const litres = amount / price;
      const addedRange = litres * efficiency;
      const targetOdo = odometer + addedRange;
      return { litres, addedRange, targetOdo, isValid: true };
    }
    return { litres: 0, addedRange: 0, targetOdo: odometer, isValid: false };
  }

  function updateForecastUI() {
    const calc = calculateForecast();
    const amount = parseFloat(inputAmountEl.value) || 0;

    if (calc.isValid) {
      forecastCardEl.style.display = 'flex';
      forecastLitresEl.textContent = `${calc.litres.toFixed(2)} L`;
      forecastRangeEl.textContent = `+${calc.addedRange.toFixed(1)} km`;
      forecastTargetOdoEl.textContent = `${formatNumber(calc.targetOdo)} km`;
      forecastTargetDescEl.textContent = `You will reach this target with your current Rs ${amount} refuel.`;
      btnSaveEl.disabled = false;
    } else {
      forecastCardEl.style.display = 'none';
      btnSaveEl.disabled = true;
    }

    const currentOdo = parseFloat(inputOdometerEl.value) || state.odometer;
    odoDisplayEl.textContent = formatNumber(currentOdo);
    priceRateBadgeEl.textContent = `Rs ${parseFloat(inputPriceEl.value) || 64.25} / L`;
  }

  function calculateStats() {
    const totalSpent = state.records.reduce((acc, r) => acc + (r.amountSpent || 0), 0);
    const totalLitres = state.records.reduce((acc, r) => acc + (r.fuelLitres || 0), 0);

    let realEfficiency = 0;
    let sub = 'Need 2+ records';

    if (state.records.length >= 2) {
      const sorted = [...state.records].sort((a, b) => a.odometerReading - b.odometerReading);
      const distance = sorted[sorted.length - 1].odometerReading - sorted[0].odometerReading;
      const fuelExceptLast = sorted.slice(0, sorted.length - 1).reduce((acc, r) => acc + r.fuelLitres, 0);

      if (fuelExceptLast > 0 && distance > 0) {
        realEfficiency = distance / fuelExceptLast;
        sub = 'Based on history';
      }
    }

    return { totalSpent, totalLitres, realEfficiency, sub };
  }

  function renderStats() {
    const stats = calculateStats();
    statTotalSpentEl.textContent = `Rs ${formatNumber(stats.totalSpent)}`;
    statAvgEfficiencyEl.textContent = stats.realEfficiency > 0 ? `${stats.realEfficiency.toFixed(1)} km/L` : '---';
    statAvgSubEl.textContent = stats.sub;
  }

  function renderHistory() {
    historyBadgeCountEl.textContent = `${state.records.length} logs`;

    if (state.records.length === 0) {
      emptyHistoryEl.style.display = 'block';
      historyListEl.innerHTML = '';
      return;
    }

    emptyHistoryEl.style.display = 'none';
    historyListEl.innerHTML = state.records.map((record) => {
      const dateFormatted = formatDate(record.date);
      return `
        <div class="log-item-card" data-id="${record.id}">
          <div>
            <div class="log-date">${dateFormatted}</div>
            <div class="log-sub">Odometer</div>
            <div class="log-odo">${formatNumber(record.odometerReading)} km</div>
          </div>
          <div>
            <div class="log-spent">Spent Rs ${formatNumber(record.amountSpent)}</div>
            <div class="log-sub">${record.fuelLitres.toFixed(2)} L @ Rs ${record.pricePerLitre.toFixed(2)}/L</div>
            ${record.notes ? `<div class="log-sub" style="font-style: italic;">• ${escapeHtml(record.notes)}</div>` : ''}
          </div>
          <div class="log-target-col">
            <div class="log-sub">Target to Reach</div>
            <div class="log-target-val">${formatNumber(record.targetOdometer)} km</div>
            <div class="log-range-badge">Range: +${record.estimatedRange.toFixed(0)}k</div>
          </div>
          <div>
            <button class="btn-delete-log" onclick="window.fuelAppDeleteLog(${record.id})" title="Delete record">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  function render() {
    updateForecastUI();
    renderStats();
    renderHistory();
  }

  function showToast(msg) {
    toastMessageEl.textContent = msg;
    toastBannerEl.classList.remove('hidden');
    setTimeout(() => {
      toastBannerEl.classList.add('hidden');
    }, 4000);
  }

  function saveRecord() {
    const amount = parseFloat(inputAmountEl.value) || 0;
    const price = parseFloat(inputPriceEl.value) || 0;
    const odometer = parseFloat(inputOdometerEl.value) || 0;
    const efficiency = parseFloat(sliderEfficiencyEl.value) || 15.0;
    const date = inputDateEl.value || new Date().toISOString().split('T')[0];
    const notes = inputNotesEl.value.trim();

    if (amount <= 0 || price <= 0 || odometer <= 0 || efficiency <= 0) {
      alert('Please fill out valid refueling details.');
      return;
    }

    const calc = calculateForecast();
    const newRecord = {
      id: Date.now(),
      date: date,
      amountSpent: amount,
      pricePerLitre: price,
      odometerReading: odometer,
      averageMileage: efficiency,
      fuelLitres: calc.litres,
      estimatedRange: calc.addedRange,
      targetOdometer: calc.targetOdo,
      notes: notes
    };

    // Prepend new record
    state.records.unshift(newRecord);
    saveRecordsToStorage();

    // Advance baseline odometer to target
    inputOdometerEl.value = Math.round(calc.targetOdo);
    state.odometer = Math.round(calc.targetOdo);
    inputNotesEl.value = '';

    showToast('Saved refueling record and updated odometer baseline!');
    render();
  }

  function deleteRecord(id) {
    if (confirm('Delete this refueling log record?')) {
      state.records = state.records.filter(r => r.id !== id);
      saveRecordsToStorage();
      render();
    }
  }
  window.fuelAppDeleteLog = deleteRecord;

  function bindEvents() {
    // Input listeners for real-time recalculation
    [inputAmountEl, inputPriceEl, inputOdometerEl].forEach(el => {
      el.addEventListener('input', updateForecastUI);
    });

    // Slider & numeric efficiency synchronization
    sliderEfficiencyEl.addEventListener('input', (e) => {
      const val = parseFloat(e.target.value);
      inputEfficiencyEl.value = val.toFixed(1);
      displayEfficiencyEl.textContent = `${val.toFixed(1)} km/L`;
      updateForecastUI();
    });

    inputEfficiencyEl.addEventListener('input', (e) => {
      const val = parseFloat(e.target.value);
      if (!isNaN(val) && val >= 5 && val <= 45) {
        sliderEfficiencyEl.value = val;
        displayEfficiencyEl.textContent = `${val.toFixed(1)} km/L`;
        updateForecastUI();
      }
    });

    // Amount preset buttons
    document.querySelectorAll('.preset-amount').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.preset-amount').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        inputAmountEl.value = btn.getAttribute('data-val');
        updateForecastUI();
      });
    });

    // Mileage preset buttons
    document.querySelectorAll('.preset-eff').forEach(btn => {
      btn.addEventListener('click', () => {
        const val = parseFloat(btn.getAttribute('data-val'));
        sliderEfficiencyEl.value = val;
        inputEfficiencyEl.value = val.toFixed(1);
        displayEfficiencyEl.textContent = `${val.toFixed(1)} km/L`;
        updateForecastUI();
      });
    });

    // Save button
    btnSaveEl.addEventListener('click', saveRecord);

    // Export Data
    btnExportEl.addEventListener('click', () => {
      const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(state.records, null, 2));
      const downloadAnchor = document.createElement('a');
      downloadAnchor.setAttribute("href", dataStr);
      downloadAnchor.setAttribute("download", `fuel_tracker_backup_${new Date().toISOString().split('T')[0]}.json`);
      document.body.appendChild(downloadAnchor);
      downloadAnchor.click();
      downloadAnchor.remove();
    });

    // Import Data
    fileImportEl.addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (event) => {
        try {
          const imported = JSON.parse(event.target.result);
          if (Array.isArray(imported)) {
            state.records = imported;
            saveRecordsToStorage();
            render();
            showToast(`Successfully restored ${imported.length} records!`);
          } else {
            alert('Invalid backup JSON format.');
          }
        } catch (err) {
          alert('Could not parse backup file.');
        }
      };
      reader.readAsText(file);
    });

    // PWA Install prompt handling
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      deferredInstallPrompt = e;
      if (btnInstallPwaEl) {
        btnInstallPwaEl.style.display = 'inline-flex';
      }
    });

    if (btnInstallPwaEl) {
      btnInstallPwaEl.addEventListener('click', async () => {
        if (deferredInstallPrompt) {
          deferredInstallPrompt.prompt();
          const { outcome } = await deferredInstallPrompt.userChoice;
          if (outcome === 'accepted') {
            btnInstallPwaEl.style.display = 'none';
          }
          deferredInstallPrompt = null;
        }
      });
    }
  }

  function registerServiceWorker() {
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('./sw.js')
          .then((reg) => console.log('PWA Service Worker registered:', reg.scope))
          .catch((err) => console.log('PWA Service Worker registration failed:', err));
      });
    }
  }

  function formatNumber(num) {
    if (isNaN(num)) return '0';
    return Number(num).toLocaleString('en-US', { maximumFractionDigits: 1 });
  }

  function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  function escapeHtml(str) {
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  // Start app on DOM ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
