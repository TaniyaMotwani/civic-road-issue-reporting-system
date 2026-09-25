import { useEffect, useMemo, useState } from 'react'

const TYPES = ['POTHOLE', 'UNEVEN_ROAD', 'OBSTRUCTION', 'CRACK', 'WATERLOGGING', 'DAMAGED_SURFACE', 'OTHER']
const STATUSES = ['OPEN', 'VERIFIED', 'IN_PROGRESS', 'RESOLVED']
const title = value => (value || '').toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, letter => letter.toUpperCase())
const date = value => value ? new Date(value).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : '—'

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {})
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (options.method && !['GET', 'HEAD', 'OPTIONS'].includes(options.method.toUpperCase())) {
    await fetch('/api/auth/csrf', { credentials: 'same-origin' })
    const csrf = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='))?.slice('XSRF-TOKEN='.length)
    if (!csrf) throw new Error('Could not prepare the secure request. Refresh the page and try again.')
    headers.set('X-XSRF-TOKEN', decodeURIComponent(csrf))
  }
  const response = await fetch(path, { ...options, headers, credentials: 'same-origin' })
  if (response.status === 204) return null
  const text = await response.text()
  let result
  try { result = text ? JSON.parse(text) : null } catch { result = null }
  if (!response.ok) throw new Error(result?.message || (response.status === 401 ? 'Please sign in to continue.' : 'The request could not be completed.'))
  return result
}

function App() {
  const [user, setUser] = useState(null)
  const [page, setPage] = useState('home')
  const [selectedId, setSelectedId] = useState(null)
  const [groups, setGroups] = useState([])
  const [dashboard, setDashboard] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function loadGroups() {
    try { setGroups(await api('/api/issues')) } catch (e) { setError(e.message) }
  }
  async function loadDashboard() {
    try { setDashboard(await api('/api/dashboard')) } catch (e) { setError(e.message) }
  }
  useEffect(() => {
    Promise.all([api('/api/auth/me').then(setUser).catch(() => {}), api('/api/issues').then(setGroups).catch(() => {}), api('/api/dashboard').then(setDashboard).catch(() => {})])
  }, [])

  function navigate(next, id = null) {
    setError(''); setNotice(''); setSelectedId(id); setPage(next); window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  async function signOut() {
    try { await api('/api/auth/logout', { method: 'POST' }); setUser(null); setNotice('You are signed out.'); navigate('home') }
    catch (e) { setError(e.message) }
  }

  return <div className="app-shell">
    <header className="site-header">
      <button className="brand" onClick={() => navigate('home')} aria-label="Roadwatch home"><span className="brand-icon">r</span><span>roadwatch</span></button>
      <nav className="main-nav" aria-label="Main navigation">
        <button className={page === 'home' || page === 'browse' || page === 'detail' ? 'active' : ''} onClick={() => navigate('browse')}>Explore issues</button>
        {user && <button className={page === 'my-reports' ? 'active' : ''} onClick={() => navigate('my-reports')}>My reports</button>}
        {user?.role === 'ADMIN' && <button className={page === 'admin' ? 'active' : ''} onClick={() => navigate('admin')}>Admin</button>}
      </nav>
      <div className="account-nav"><button className="header-report" aria-label="Report a road issue" onClick={() => navigate('report')}>Report issue <span>↗</span></button>{user ? <><span className="user-label">{user.email}</span><button className="quiet-button" onClick={signOut}>Sign out</button></> : <button className="quiet-button" onClick={() => navigate('account')}>Sign in</button>}</div>
    </header>
    {error && <div className="toast error" role="alert">{error}<button onClick={() => setError('')} aria-label="Dismiss">×</button></div>}
    {notice && <div className="toast success" role="status">{notice}<button onClick={() => setNotice('')} aria-label="Dismiss">×</button></div>}

    {page === 'home' && <Home dashboard={dashboard} groups={groups} onBrowse={() => navigate('browse')} onReport={() => navigate('report')} onOpen={id => navigate('detail', id)} />}
    {page === 'browse' && <Browse groups={groups} onOpen={id => navigate('detail', id)} />}
    {page === 'account' && <Account onAuthenticated={async nextUser => { setUser(nextUser); setNotice('Welcome to Roadwatch.'); await loadGroups(); navigate('home') }} />}
    {page === 'report' && <ReportForm onDone={async report => { setNotice(`Report ${report.code} submitted${report.issueCode ? ` under ${report.issueCode}` : ''}.`); await Promise.all([loadGroups(), loadDashboard()]); navigate('my-reports') }} />}
    {page === 'my-reports' && user && <MyReports onOpen={id => navigate('detail', id)} />}
    {page === 'detail' && <IssueDetail id={selectedId} user={user} onBack={() => navigate('browse')} onUpdated={async () => { await Promise.all([loadGroups(), loadDashboard()]); setNotice('Issue details updated.') }} />}
    {page === 'admin' && user?.role === 'ADMIN' && <AdminDashboard data={dashboard} groups={groups} onOpen={id => navigate('detail', id)} refresh={async () => { await Promise.all([loadGroups(), loadDashboard()]) }} />}
    {page === 'report' && !user && <Account onAuthenticated={nextUser => { setUser(nextUser); navigate('report') }} />}

    <footer className="site-footer"><button className="footer-brand" onClick={() => navigate('home')}>ROADWATCH</button><span>Community insight for safer streets.</span><span>Location is requested only when you choose to report an issue.</span></footer>
  </div>
}

function Home({ dashboard, groups, onBrowse, onReport, onOpen }) {
  return <main>
    <section className="hero">
      <div className="hero-copy"><div className="eyebrow"><span /> A COMMUNITY VIEW OF OUR STREETS</div><h1>Small road issues<br />deserve <em>to be seen.</em></h1><p>Share a road problem, help neighbors find it, and make recurring issues easier to understand.</p><div className="hero-actions"><button className="primary-button" onClick={onReport}>Report a road issue <span>↗</span></button><button className="text-button" onClick={onBrowse}>Explore reported issues <span>→</span></button></div><div className="privacy-note"><span className="privacy-dot" /> Your location is used only when you choose to report.</div></div>
      <div className="hero-art" aria-label="Illustrated neighborhood road"><div className="art-sun"/><div className="art-hill"/><div className="art-building b-one"><i/><i/><i/><i/></div><div className="art-building b-two"><i/><i/><i/></div><div className="art-tree t-one"><i/><b/></div><div className="art-tree t-two"><i/><b/></div><div className="art-sidewalk"/><div className="art-road"><span/><span/></div><div className="art-caption"><b>⌖</b><span>BETTER STREETS, TOGETHER</span></div></div>
    </section>
    <section className="stats-strip" aria-label="Community reports"><div><strong>{dashboard?.reports ?? '—'}</strong><span>citizen reports</span></div><div><strong>{dashboard?.groups ?? '—'}</strong><span>grouped road issues</span></div><div><strong>{dashboard?.inProgress ?? '—'}</strong><span>in progress</span></div><button onClick={onBrowse}>View community issues <span>→</span></button></section>
    <section className="section-block"><div className="section-heading"><div><div className="eyebrow">LATEST COMMUNITY ACTIVITY</div><h2>Issues near the community</h2></div><button className="text-button" onClick={onBrowse}>See all issues <span>→</span></button></div>
      {groups.length ? <div className="group-grid">{groups.slice(0, 3).map(group => <GroupCard key={group.id} group={group} onClick={() => onOpen(group.id)} />)}</div> : <div className="empty-card"><span className="empty-mark">⌖</span><h3>A clearer picture starts here</h3><p>No road issues have been reported yet. Start by sharing what you see on your street.</p><button className="primary-button small" onClick={onReport}>Make the first report <span>↗</span></button></div>}
    </section>
    <section className="how-section"><div><div className="eyebrow">A SIMPLE, SHARED RECORD</div><h2>From one report<br />to a clearer picture.</h2></div><div className="how-steps"><article><span>01</span><h3>Share what you see</h3><p>Add a photo and allow location access when you are ready to report.</p></article><article><span>02</span><h3>Reports come together</h3><p>Nearby reports of the same issue can be grouped into one community issue.</p></article><article><span>03</span><h3>Follow its progress</h3><p>See the issue status and how many neighbors have reported it.</p></article></div></section>
  </main>
}

function GroupCard({ group, onClick }) {
  return <button className="group-card" onClick={onClick}><div className="group-card-top"><span className="type-tag">{title(group.issueType)}</span><StatusTag status={group.status} /></div><h3>{group.code}</h3><p className="group-area">{group.area || group.address || `${group.latitude.toFixed(4)}, ${group.longitude.toFixed(4)}`}</p><div className="group-card-bottom"><span>{group.reportCount} {group.reportCount === 1 ? 'report' : 'reports'}</span><span>{date(group.updatedAt)}</span><b aria-hidden="true">↗</b></div></button>
}

function StatusTag({ status }) { return <span className={`status-tag ${String(status || '').toLowerCase()}`}>{title(status)}</span> }

function Browse({ groups, onOpen }) {
  const [area, setArea] = useState('')
  const [type, setType] = useState('')
  const [severity, setSeverity] = useState('')
  const [status, setStatus] = useState('')
  const filtered = useMemo(() => groups.filter(g => (!area || `${g.area || ''} ${g.address || ''}`.toLowerCase().includes(area.toLowerCase())) && (!type || g.issueType === type) && (!severity || g.severity === severity) && (!status || g.status === status)), [groups, area, type, severity, status])
  const areas = [...new Set(groups.map(g => g.area).filter(Boolean))].sort()
  return <main className="content-page"><PageHeading eyebrow="AREA · ISSUE · REPORTS" title="Explore road issues" description="Browse grouped reports by area and see what neighbors have noticed." />
    <div className="filter-panel"><label>Area<input value={area} onChange={e => setArea(e.target.value)} placeholder="Search locality or address" list="area-options"/><datalist id="area-options">{areas.map(x => <option key={x} value={x}/>)}</datalist></label><label>Issue type<select value={type} onChange={e => setType(e.target.value)}><option value="">All types</option>{TYPES.map(x => <option key={x} value={x}>{title(x)}</option>)}</select></label><label>Severity<select value={severity} onChange={e => setSeverity(e.target.value)}><option value="">All severities</option>{['LOW','MEDIUM','HIGH','UNKNOWN'].map(x => <option key={x} value={x}>{title(x)}</option>)}</select></label><label>Status<select value={status} onChange={e => setStatus(e.target.value)}><option value="">All statuses</option>{STATUSES.map(x => <option key={x} value={x}>{title(x)}</option>)}</select></label><span className="result-count">{filtered.length} {filtered.length === 1 ? 'issue' : 'issues'}</span></div>
    {filtered.length ? <div className="issue-list">{filtered.map(g => <GroupCard key={g.id} group={g} onClick={() => onOpen(g.id)} />)}</div> : <EmptyState title="No matching issues" text="Try clearing a filter, or report a road issue that is not listed." />}
  </main>
}

function PageHeading({ eyebrow, title: heading, description }) { return <div className="page-heading"><div className="eyebrow">{eyebrow}</div><h1>{heading}</h1><p>{description}</p></div> }
function EmptyState({ title: heading, text }) { return <div className="empty-card compact"><span className="empty-mark">⌖</span><h3>{heading}</h3><p>{text}</p></div> }

function Account({ onAuthenticated }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      if (mode === 'register') await api('/api/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) })
      const user = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
      onAuthenticated(user)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <main className="content-page account-page"><div className="account-card"><div className="eyebrow">YOUR COMMUNITY ACCOUNT</div><h1>{mode === 'login' ? 'Welcome back.' : 'Join the community.'}</h1><p>Sign in to submit a report and keep track of issues you have shared.</p><form onSubmit={submit} className="stack-form"><label>Email address<input autoComplete="email" type="email" required maxLength="254" value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com"/></label><label>Password<input autoComplete={mode === 'login' ? 'current-password' : 'new-password'} type="password" required minLength="10" maxLength="72" value={password} onChange={e => setPassword(e.target.value)} placeholder="At least 10 characters"/></label>{mode === 'register' && <small>Use at least 10 characters. Your password is stored as a one-way hash.</small>}{error && <div className="form-error" role="alert">{error}</div>}<button className="primary-button" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}<span>→</span></button></form><p className="switch-mode">{mode === 'login' ? 'New to Roadwatch?' : 'Already have an account?'} <button onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError('') }}>{mode === 'login' ? 'Create an account' : 'Sign in'}</button></p></div></main>
}

function ReportForm({ onDone }) {
  const [type, setType] = useState('POTHOLE')
  const [description, setDescription] = useState('')
  const [image, setImage] = useState(null)
  const [preview, setPreview] = useState('')
  const [location, setLocation] = useState(null)
  const [locating, setLocating] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview) }, [preview])
  async function locate() {
    if (!navigator.geolocation) { setError('This browser does not support location. You can still report by using the map pin prompt in a later update.'); return }
    setLocating(true); setError(''); setLocation(null)
    navigator.geolocation.getCurrentPosition(async position => {
      const point = { latitude: position.coords.latitude, longitude: position.coords.longitude, address: '', area: '' }
      try {
        const result = await api(`/api/locations/reverse?latitude=${point.latitude}&longitude=${point.longitude}`)
        point.address = result.address || ''; point.area = result.area || ''
      } catch { /* Keep GPS coordinates usable if optional address lookup is unavailable. */ }
      setLocation(point); setLocating(false)
    }, issue => {
      setError(issue.code === 1 ? 'Location permission was denied. Allow location access in browser settings and try again.' : 'Could not get your location. Check that device location is enabled and try again.')
      setLocating(false)
    }, { enableHighAccuracy: true, timeout: 12000, maximumAge: 60000 })
  }
  function chooseImage(file) {
    setPreview(current => { if (current) URL.revokeObjectURL(current); return file ? URL.createObjectURL(file) : '' })
    setImage(file)
  }
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      if (!image) throw new Error('Add a clear photo of the road issue.')
      if (!location) throw new Error('Share the issue location before submitting.')
      const body = new FormData()
      body.set('issueType', type); body.set('description', description)
      body.set('latitude', String(location.latitude)); body.set('longitude', String(location.longitude))
      body.set('address', location.address); body.set('area', location.area); body.set('image', image)
      const report = await api('/api/reports', { method: 'POST', body })
      onDone(report)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <main className="content-page"><PageHeading eyebrow="NEW COMMUNITY REPORT" title="What did you notice?" description="A photo and a location help neighbors understand the same road problem." />
    <form className="report-layout" onSubmit={submit}>
      <section className="form-card"><div className="form-section-title"><span>01</span><div><h2>Describe the issue</h2><p>Choose the closest type. You can add context below.</p></div></div><label>Issue type<select value={type} onChange={e => setType(e.target.value)}>{TYPES.map(x => <option key={x} value={x}>{title(x)}</option>)}</select></label><label>Anything else we should know? <span className="optional">Optional</span><textarea value={description} onChange={e => setDescription(e.target.value)} rows="4" maxLength="1000" placeholder="For example, mention a nearby landmark or how the issue affects the road."/></label>
      <div className="form-section-title separated"><span>02</span><div><h2>Add a photo</h2><p>Use a JPEG or PNG image, up to 8 MB.</p></div></div><label className="upload-box">{preview ? <img src={preview} alt="Selected road issue preview"/> : <><span className="upload-icon">＋</span><b>Choose a photo</b><span>or take one with your device camera</span></>}<input type="file" accept="image/jpeg,image/png" capture="environment" onChange={e => { chooseImage(e.target.files?.[0] || null); e.target.value = '' }} required={!image}/></label>{image && <button className="remove-file" type="button" onClick={() => chooseImage(null)}>Remove {image.name}</button>}
      <div className="form-section-title separated"><span>03</span><div><h2>Pinpoint the location</h2><p>Your location is requested only for this report.</p></div></div><div className="location-callout"><div className="location-symbol">⌖</div><div><b>Use your current location</b><p>Roadwatch uses your device location to show where the issue is. It is not tracked in the background.</p></div><button type="button" className="outline-button" disabled={locating} onClick={locate}>{locating ? 'Finding…' : location ? 'Update location' : 'Share location'}</button></div>{location && <div className="location-result"><span className="location-check">✓</span><div><b>{location.area || 'Location captured'}</b><span>{location.address || `${location.latitude.toFixed(5)}, ${location.longitude.toFixed(5)}`}</span></div><small>{location.latitude.toFixed(5)}, {location.longitude.toFixed(5)}</small></div>}
      {error && <div className="form-error" role="alert">{error}</div>}<div className="form-submit"><span>By submitting, you confirm this photo relates to a road concern.</span><button className="primary-button" disabled={busy}>{busy ? 'Submitting…' : 'Submit report'} <span>→</span></button></div>
    </section>
    <aside className="report-aside"><div className="aside-sticky"><div className="aside-mark">⌖</div><h3>What happens next?</h3><p>Reports that appear to describe the same nearby issue may be grouped together. An optional AI assessment can help prioritize review; people remain responsible for decisions.</p><div className="aside-rule"/><div className="aside-fact"><span>01</span><p>Your photo and location are saved with the report.</p></div><div className="aside-fact"><span>02</span><p>You receive a report ID and can see its current status.</p></div><div className="aside-fact"><span>03</span><p>Your account lets you return to your reports.</p></div></div></aside>
    </form>
  </main>
}

function MyReports({ onOpen }) {
  const [reports, setReports] = useState([])
  const [error, setError] = useState('')
  useEffect(() => { api('/api/my/reports').then(setReports).catch(e => setError(e.message)) }, [])
  return <main className="content-page"><PageHeading eyebrow="YOUR CONTRIBUTION" title="My reports" description="Follow the road issues you have shared with the community." />{error && <div className="form-error">{error}</div>}{reports.length ? <div className="personal-report-list">{reports.map(report => <button className="personal-report" key={report.id} onClick={() => onOpen(report.issueGroupId)}><img src={report.imageUrl} alt=""/><div><span className="type-tag">{title(report.issueType)}</span><h3>{report.code}</h3><p>{report.area || report.address || 'Location captured'} · {date(report.createdAt)}</p><span className="report-linked">Grouped under {report.issueCode}</span></div><StatusTag status={report.status}/></button>)}</div> : !error && <EmptyState title="No reports yet" text="When you submit a road issue, you can follow it here."/>}</main>
}

function IssueDetail({ id, user, onBack, onUpdated }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [status, setStatus] = useState('')
  const [note, setNote] = useState('')
  const [review, setReview] = useState({})
  const [busy, setBusy] = useState(false)
  async function reload() { try { setData(await api(`/api/issues/${id}`)); setError('') } catch (e) { setError(e.message) } }
  useEffect(() => { reload() }, [id])
  if (!data) return <main className="content-page"><button className="back-button" onClick={onBack}>← Back to issues</button>{error ? <div className="form-error">{error}</div> : <div className="loading-panel">Loading issue…</div>}</main>
  const issue = data.issue
  async function updateStatus(event) {
    event.preventDefault(); setBusy(true)
    try { await api(`/api/admin/issues/${issue.id}/status`, { method: 'PUT', body: JSON.stringify({ status, note }) }); await reload(); onUpdated() } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function reviewReport(report) {
    setBusy(true)
    try { await api(`/api/admin/reports/${report.id}/verification`, { method: 'PUT', body: JSON.stringify(review[report.id] || { state: 'VERIFIED', note: 'Reviewed by administrator' }) }); await reload(); onUpdated() } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <main className="content-page"><button className="back-button" onClick={onBack}>← All road issues</button><div className="detail-heading"><div><div className="eyebrow">COMMUNITY ISSUE · {issue.code}</div><h1>{title(issue.issueType)}</h1><p>{issue.area || issue.address || 'Area not identified'}</p></div><StatusTag status={issue.status}/></div>
    <div className="detail-grid"><section className="detail-main"><div className="detail-facts"><div><span>COMMUNITY REPORTS</span><b>{issue.reportCount}</b></div><div><span>SEVERITY</span><b>{title(issue.severity || 'UNKNOWN')}</b></div><div><span>FIRST REPORTED</span><b>{date(issue.createdAt)}</b></div></div><div className="detail-location"><span className="location-symbol">⌖</span><div><b>{issue.area || issue.address || 'Location captured'}</b><span>{issue.address || `${issue.latitude.toFixed(5)}, ${issue.longitude.toFixed(5)}`}</span></div><a href={`https://www.openstreetmap.org/?mlat=${issue.latitude}&mlon=${issue.longitude}#map=18/${issue.latitude}/${issue.longitude}`} target="_blank" rel="noreferrer">Open in OpenStreetMap ↗</a></div><div className="subsection-heading"><h2>Citizen reports</h2><span>{data.reports.length} reports</span></div><div className="detail-reports">{data.reports.map(report => <article className="detail-report" key={report.id}><img src={report.imageUrl} alt={`Photo submitted with report ${report.code}`}/><div className="detail-report-body"><div className="report-meta"><span>{report.code}</span><span>{date(report.createdAt)}</span></div><p>{report.description || 'No additional description provided.'}</p><div className="verification-line"><b>Automated assessment:</b> {title(report.verificationState)}{report.severity && report.severity !== 'UNKNOWN' ? ` · ${title(report.severity)} severity` : ''}</div>{report.verificationNote && <small>{report.verificationNote}</small>}{user?.role === 'ADMIN' && <div className="review-controls"><select aria-label="AI review decision" value={review[report.id]?.state || report.verificationState} onChange={e => setReview({ ...review, [report.id]: { ...review[report.id], state: e.target.value } })}>{['NEEDS_REVIEW','VERIFIED','NOT_A_ROAD_ISSUE'].map(x => <option key={x} value={x}>{title(x)}</option>)}</select><input aria-label="Review note" placeholder="Review note" value={review[report.id]?.note ?? ''} onChange={e => setReview({ ...review, [report.id]: { ...review[report.id], note: e.target.value } })}/><button className="outline-button small" disabled={busy} onClick={() => reviewReport(report)}>Save review</button></div>}</div></article>)}</div></section>
    <aside className="detail-aside"><div className="aside-panel"><div className="eyebrow">ISSUE STATUS</div><h3>{title(issue.status)}</h3><p>Administrators update this shared issue as work progresses.</p>{user?.role === 'ADMIN' && <form className="status-form" onSubmit={updateStatus}><label>Update status<select required value={status || issue.status} onChange={e => setStatus(e.target.value)}>{STATUSES.map(x => <option key={x} value={x}>{title(x)}</option>)}</select></label><label>Update note<input value={note} onChange={e => setNote(e.target.value)} maxLength="500" placeholder="Optional note"/></label><button className="primary-button" disabled={busy}>Save status <span>→</span></button></form>}</div><div className="aside-panel history-panel"><h3>Status history</h3>{data.statusHistory.length ? data.statusHistory.map((entry,i) => <div className="history-item" key={i}><span className="history-dot"/><div><b>{title(entry.newStatus)}</b><small>{date(entry.createdAt)} · {entry.changedBy}</small>{entry.note && <p>{entry.note}</p>}</div></div>) : <p>No status changes have been recorded yet.</p>}</div></aside></div>{error && <div className="form-error">{error}</div>}</main>
}

function AdminDashboard({ data, groups, onOpen }) {
  const [area, setArea] = useState('')
  const [type, setType] = useState('')
  const [severity, setSeverity] = useState('')
  const [status, setStatus] = useState('')
  const filtered = useMemo(() => groups.filter(g => (!area || `${g.area || ''} ${g.address || ''}`.toLowerCase().includes(area.toLowerCase())) && (!type || g.issueType === type) && (!severity || g.severity === severity) && (!status || g.status === status)), [groups, area, type, severity, status])
  const byArea = aggregate(groups, g => g.area || 'Area not identified')
  const byType = aggregate(groups, g => g.issueType)
  const bySeverity = aggregate(groups, g => g.severity || 'UNKNOWN')
  const counts = data || { reports: 0, groups: 0, open: 0, verified: 0, inProgress: 0, resolved: 0 }
  return <main className="content-page"><PageHeading eyebrow="ADMINISTRATION · AREA OVERVIEW" title="Road issue dashboard" description="Review grouped issues, citizen reports, verification assessments and progress."/><div className="admin-stats"><Metric label="Citizen reports" value={counts.reports}/><Metric label="Issue groups" value={counts.groups}/><Metric label="Open" value={counts.open}/><Metric label="Verified" value={counts.verified}/><Metric label="In progress" value={counts.inProgress}/><Metric label="Resolved" value={counts.resolved}/></div><div className="admin-breakdowns"><Breakdown title="By area" items={byArea}/><Breakdown title="By issue type" items={byType}/><Breakdown title="By severity" items={bySeverity}/></div><div className="section-heading admin-list-heading"><div><div className="eyebrow">AREA → ISSUE → REPORTS</div><h2>Grouped road issues</h2></div><span>{filtered.length} of {groups.length}</span></div><div className="filter-panel admin-filters"><label>Area<input value={area} onChange={e => setArea(e.target.value)} placeholder="Search area"/></label><label>Issue type<select value={type} onChange={e => setType(e.target.value)}><option value="">All types</option>{TYPES.map(x => <option key={x} value={x}>{title(x)}</option>)}</select></label><label>Severity<select value={severity} onChange={e => setSeverity(e.target.value)}><option value="">All severities</option>{['LOW','MEDIUM','HIGH','UNKNOWN'].map(x => <option key={x} value={x}>{title(x)}</option>)}</select></label><label>Status<select value={status} onChange={e => setStatus(e.target.value)}><option value="">All statuses</option>{STATUSES.map(x => <option key={x} value={x}>{title(x)}</option>)}</select></label></div>{filtered.length ? <div className="admin-issue-list">{filtered.map(group => <button className="admin-issue-row" key={group.id} onClick={() => onOpen(group.id)}><span className="admin-issue-code">{group.code}</span><div><b>{title(group.issueType)}</b><span>{group.area || group.address || `${group.latitude.toFixed(4)}, ${group.longitude.toFixed(4)}`}</span></div><span className="report-count"><b>{group.reportCount}</b> reports</span><StatusTag status={group.status}/><span className="row-arrow">↗</span></button>)}</div> : <EmptyState title={groups.length ? 'No matching issues' : 'No issues to review'} text={groups.length ? 'Try adjusting the filters.' : 'Community issue groups appear here after the first report.'}/>}</main>
}
function Metric({ label, value }) { return <div className="metric-card"><span>{label}</span><strong>{value ?? '—'}</strong></div> }
function aggregate(items, keyOf) {
  const counts = new Map()
  items.forEach(item => { const key = keyOf(item); counts.set(key, (counts.get(key) || 0) + 1) })
  return [...counts].map(([label, count]) => ({ label, count })).sort((a, b) => b.count - a.count || a.label.localeCompare(b.label))
}
function Breakdown({ title: heading, items }) {
  return <section className="breakdown-card"><h3>{heading}</h3>{items.length ? <ul>{items.slice(0, 6).map(item => <li key={item.label}><span>{title(item.label)}</span><b>{item.count}</b></li>)}</ul> : <p>No grouped issues yet.</p>}</section>
}

export default App
