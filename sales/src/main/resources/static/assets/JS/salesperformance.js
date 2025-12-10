document.addEventListener("DOMContentLoaded", () => {

  /* ------------------- DASHBOARD LOGIC -------------------- */

  const TEAMS = ["Backend", "Frontend", "Full Stack", "DevOps", "QA"];
  const today = new Date();

  function dateToYMD(d){
    const y = d.getFullYear();
    const m = String(d.getMonth()+1).padStart(2,'0');
    const day = String(d.getDate()).padStart(2,'0');
    return `${y}-${m}-${day}`;
  }

  // 6-month random demo data
  const startMonthDate = new Date(today.getFullYear(), today.getMonth() - 5, 1);
  const endMonthDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
  let allDates = [];
  let cur = new Date(startMonthDate);
  while(cur <= endMonthDate){
    allDates.push(dateToYMD(cur));
    cur.setDate(cur.getDate() + 1);
  }

  function randCount(){ return Math.floor(Math.random()*3); }

  const dashboardData = TEAMS.map(team => ({
    team,
    projects: allDates.map(d => ({ date: d, count: randCount() }))
  }));

  document.getElementById("totalTeams").textContent = dashboardData.length;

  /* ---------------- FILTERS ---------------- */

  const yearSelect  = document.getElementById("filterYear");
  const monthSelect = document.getElementById("filterMonth");
  const teamSelect  = document.getElementById("filterTeam");

  teamSelect.innerHTML = `<option value="All Teams">All Teams</option>` +
    dashboardData.map(t => `<option value="${t.team}">${t.team}</option>`).join('');

  // Years 2025 - 2030
  let years = [];
  for(let y=2025;y<=2030;y++){ years.push(y); }
  yearSelect.innerHTML = years.map(y=>`<option value="${y}">${y}</option>`).join('');

  // Months
  const months=[];
  for(let m=1;m<=12;m++){
    const name=new Date(2000,m-1,1).toLocaleString(undefined,{month:'long'});
    months.push({num:m,name});
  }
  monthSelect.innerHTML = `<option value="All">All Months</option>` +
    months.map(m=>`<option value="${m.num}">${String(m.num).padStart(2,'0')} — ${m.name}</option>`).join('');

  let barChart=null, lineChart=null;

  function getLastDayOfMonth(year, month){
    return new Date(year, month, 0).getDate();
  }

  function monthDayLabels(year, month){
    const last = getLastDayOfMonth(year, month);
    const dayLabels=[], isoDates=[];
    for(let d=1;d<=last;d++){
      dayLabels.push(String(d));
      const mm = String(month).padStart(2,'0');
      const dd = String(d).padStart(2,'0');
      isoDates.push(`${year}-${mm}-${dd}`);
    }
    return {dayLabels, isoDates};
  }

  function monthLabelsYear(year){
    const labels=[], iso=[];
    for(let m=1;m<=12;m++){
      const name=new Date(year,m-1,1).toLocaleString(undefined,{month:'short'});
      labels.push(name);
      iso.push(`${year}-${String(m).padStart(2,'0')}`);
    }
    return {labels, iso};
  }

  function createCharts(teamFilter, year, month){
    year = Number(year);
    const monthName = (month !== 'All')
      ? new Date(year, month-1, 1).toLocaleString(undefined,{month:'long'})
      : null;

    /* ---------------- LINE CHART ---------------- */
    let lineLabels, lineValues;
    if(month === 'All'){
      const {labels, iso} = monthLabelsYear(year);
      lineLabels = labels;
      lineValues = iso.map(isoMonth => {
        return dashboardData.reduce((acc, teamObj)=>{
          const sum = teamObj.projects.reduce((s,p)=>
            p.date.startsWith(isoMonth) ? s+p.count : s, 0);
          return acc + (teamFilter==='All Teams' ? sum : (teamObj.team===teamFilter ? sum : 0));
        },0);
      });
    } else {
      const {dayLabels, isoDates} = monthDayLabels(year, Number(month));
      lineLabels = dayLabels;
      lineValues = isoDates.map(dateStr => {
        if(teamFilter==='All Teams'){
          return dashboardData.reduce((s,teamObj)=>{
            return s + (teamObj.projects.find(p=>p.date===dateStr)?.count || 0);
          },0);
        }
        const teamObj = dashboardData.find(t=>t.team===teamFilter);
        return teamObj?.projects.find(p=>p.date===dateStr)?.count||0;
      });
    }

    /* ---------------- BAR CHART ---------------- */
    const barLabels = dashboardData.map(t=>t.team);
    let barValues;

    if(month === 'All'){
      barValues = dashboardData.map(teamObj => {
        return teamObj.projects.reduce((s,p)=> p.date.startsWith(String(year)) ? s+p.count : s, 0);
      });
    } else {
      const {isoDates} = monthDayLabels(year, Number(month));
      barValues = dashboardData.map(teamObj => {
        return teamObj.projects
          .filter(p=> isoDates.includes(p.date))
          .reduce((s,p)=>s+p.count,0);
      });
    }

    const displayBarValues =
      teamFilter==='All Teams'
        ? barValues
        : barLabels.map((lbl,i)=> lbl===teamFilter ? barValues[i] : 0);

    /* ---------------- Set Counts ---------------- */
    const totalProjects = lineValues.reduce((s,v)=>s+v,0);
    document.getElementById("totalProjects").textContent = totalProjects;

    /* ---------------- Titles ---------------- */
    if(month === 'All'){
      document.getElementById("barChartTitle").innerHTML =
        `Project Distribution to IT Teams in <span style="color:#007bff">${year}</span>`;

      document.getElementById("lineChartTitle").innerHTML =
        `Project Allocation by Month — <span style="color:#ff4d4d">${year}</span>`;
    } else {
      document.getElementById("barChartTitle").innerHTML =
        `Project Distribution — <span style="color:#007bff">${monthName}</span>`;

      document.getElementById("lineChartTitle").innerHTML =
        `Project Allocation — <span style="color:#ff4d4d">${monthName}</span>`;
    }

    /* ---------------- Render Charts ---------------- */
    if(barChart) barChart.destroy();
    if(lineChart) lineChart.destroy();

    const ctxBar = document.getElementById("barChart").getContext("2d");
    barChart = new Chart(ctxBar,{
      type:'bar',
      data:{ labels:barLabels, datasets:[{
        label:'Projects',
        data:displayBarValues,
        backgroundColor:'#007bff',
        borderRadius:6
      }] }
    });

    const ctxLine = document.getElementById("lineChart").getContext("2d");
    lineChart = new Chart(ctxLine,{
      type:'line',
      data:{ labels:lineLabels, datasets:[{
        label:'Projects',
        data:lineValues,
        borderColor:'#ff4d4d',
        backgroundColor:'rgba(255,77,77,0.2)',
        fill:true,
        tension:0.3
      }] }
    });
  }

  /* ---------------- Initial Charts ---------------- */
  yearSelect.value = today.getFullYear();
  monthSelect.value = today.getMonth()+1;
  createCharts("All Teams", yearSelect.value, monthSelect.value);

  /* ---------------- Filter Buttons ---------------- */
  document.getElementById("applyFilter").addEventListener("click", ()=>{
    createCharts(teamSelect.value, yearSelect.value, monthSelect.value);
  });

  document.getElementById("resetFilter").addEventListener("click", ()=>{
    yearSelect.value = today.getFullYear();
    monthSelect.value = today.getMonth()+1;
    teamSelect.value = "All Teams";
    createCharts("All Teams", yearSelect.value, monthSelect.value);
  });


  /* ---------------- Resource Allocation Form ---------------- */

  const form = document.getElementById("resource_allocation_form");
  if (form) {

    form.addEventListener("submit", async (e) => {
      e.preventDefault();

      const projectId = document.getElementById("project_id")?.value?.trim() || null;
      const itTeam = document.getElementById("it_team")?.value?.trim() || null;
      const startDate = document.getElementById("start_date")?.value || null;
      const endDate = document.getElementById("end_date")?.value || null;

      if (!projectId || !itTeam || !startDate || !endDate) {
        alert("Please fill all fields.");
        return;
      }

      const token = localStorage.getItem("token") || localStorage.getItem("authToken");

      if (!token) {
        alert("Authentication missing!");
        return;
      }

      const data = { projectId, itTeam, startDate, endDate };

      const response = await fetch("https://sales-erp-project-1.up.railway.app:8080/api/allocations", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify(data)
      });

      if (!response.ok) {
        const text = await response.text();
        console.error("API ERROR:", text);
        alert("Failed to save allocation.");
        return;
      }

      alert("Resource Allocation saved successfully!");
      form.reset();
    });
  }

});

