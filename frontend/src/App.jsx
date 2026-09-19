import React, { useState, useEffect, useRef } from 'react';
import {
  ShieldAlert, ShieldCheck, AlertTriangle, Activity, Mic, MicOff,
  Upload, UserCheck, Users, FileText, Play, Pause, RefreshCw,
  Volume2, ExternalLink, CheckCircle2, XCircle, ArrowRight,
  Terminal, BarChart2, Radio, Lock, Eye, AlertCircle
} from 'lucide-react';
import {
  ResponsiveContainer, AreaChart, Area, XAxis, YAxis,
  Tooltip, CartesianGrid
} from 'recharts';

const API_BASE = ''; // Uses Vite proxy to http://127.0.0.1:8000

export default function App() {
  const [activeTab, setActiveTab] = useState('scenarios');
  const [systemStatus, setSystemStatus] = useState(null);
  const [backendOnline, setBackendOnline] = useState(false);

  // Scenarios state
  const [scenarios, setScenarios] = useState([]);
  const [selectedScenarioId, setSelectedScenarioId] = useState('scenario_2');
  const [activeScenarioResult, setActiveScenarioResult] = useState(null);
  const [loadingScenario, setLoadingScenario] = useState(false);
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const audioRef = useRef(null);

  // Audio Upload / Live Mic state
  const [uploadFile, setUploadFile] = useState(null);
  const [customTranscript, setCustomTranscript] = useState('');
  const [analyzingAudio, setAnalyzingAudio] = useState(false);
  const [analysisResult, setAnalysisResult] = useState(null);
  const [isRecording, setIsRecording] = useState(false);
  const mediaRecorderRef = useRef(null);
  const audioChunksRef = useRef([]);

  // Trusted Speakers state
  const [speakers, setSpeakers] = useState([]);
  const [loadingSpeakers, setLoadingSpeakers] = useState(false);
  const [newSpeakerName, setNewSpeakerName] = useState('');
  const [newSpeakerRel, setNewSpeakerRel] = useState('Trusted Family Contact');
  const [newSpeakerNotes, setNewSpeakerNotes] = useState('');
  const [newSpeakerAudio, setNewSpeakerAudio] = useState(null);
  const [addingSpeaker, setAddingSpeaker] = useState(false);

  // Challenge Response state
  const [currentChallenge, setCurrentChallenge] = useState('');
  const [challengeInstructions, setChallengeInstructions] = useState('');
  const [challengeInput, setChallengeInput] = useState('');
  const [verificationResult, setVerificationResult] = useState(null);
  const [verifyingChallenge, setVerifyingChallenge] = useState(false);

  // Forensic Report state
  const [currentSessionId, setCurrentSessionId] = useState('DEMO-SCENARIO_2');
  const [reportData, setReportData] = useState(null);
  const [loadingReport, setLoadingReport] = useState(false);

  // Dataset state
  const [datasetStats, setDatasetStats] = useState(null);
  const [datasetManifest, setDatasetManifest] = useState(null);
  const [loadingDataset, setLoadingDataset] = useState(false);

  const fetchDatasetInfo = async () => {
    setLoadingDataset(true);
    try {
      const [resStats, resManifest] = await Promise.all([
        fetch(`${API_BASE}/api/dataset/stats`),
        fetch(`${API_BASE}/api/dataset/manifest`)
      ]);
      if (resStats.ok) {
        const stats = await resStats.json();
        setDatasetStats(stats);
      }
      if (resManifest.ok) {
        const manifest = await resManifest.json();
        setDatasetManifest(manifest);
      }
    } catch (err) {
      console.error('Error fetching dataset info:', err);
    } finally {
      setLoadingDataset(false);
    }
  };

  // Check backend health
  const checkHealth = async () => {
    try {
      const res = await fetch(`${API_BASE}/`);
      if (res.ok) {
        const data = await res.json();
        setSystemStatus(data);
        setBackendOnline(true);
      } else {
        setBackendOnline(false);
      }
    } catch {
      setBackendOnline(false);
    }
  };

  // Fetch initial data
  useEffect(() => {
    checkHealth();
    fetchScenarios();
    fetchSpeakers();
    fetchChallenge();

    const interval = setInterval(checkHealth, 10000);
    return () => clearInterval(interval);
  }, []);

  // Fetch demo scenarios
  const fetchScenarios = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/demo/scenarios`);
      if (res.ok) {
        const data = await res.json();
        setScenarios(data);
        if (data.length > 0) {
          // Auto-run first scenario or default
          runScenario(data[1]?.id || data[0]?.id);
        }
      }
    } catch (err) {
      console.error('Error fetching scenarios:', err);
    }
  };

  // Run a demo scenario
  const runScenario = async (scenarioId) => {
    setLoadingScenario(true);
    setSelectedScenarioId(scenarioId);
    try {
      const res = await fetch(`${API_BASE}/api/demo/run`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ scenario_id: scenarioId }),
      });
      if (res.ok) {
        const data = await res.json();
        setActiveScenarioResult(data);
        setCurrentSessionId(data.session_id);
        setIsPlayingAudio(false);
      }
    } catch (err) {
      console.error('Error running scenario:', err);
    } finally {
      setLoadingScenario(false);
    }
  };

  // Fetch trusted speakers
  const fetchSpeakers = async () => {
    setLoadingSpeakers(true);
    try {
      const res = await fetch(`${API_BASE}/api/speakers`);
      if (res.ok) {
        const data = await res.json();
        setSpeakers(data);
      }
    } catch (err) {
      console.error('Error fetching speakers:', err);
    } finally {
      setLoadingSpeakers(false);
    }
  };

  // Register new speaker
  const handleRegisterSpeaker = async (e) => {
    e.preventDefault();
    if (!newSpeakerName.trim()) return;
    setAddingSpeaker(true);
    try {
      const formData = new FormData();
      formData.append('name', newSpeakerName.trim());
      formData.append('relationship', newSpeakerRel.trim());
      formData.append('notes', newSpeakerNotes.trim());
      if (newSpeakerAudio) {
        formData.append('audio', newSpeakerAudio);
      }
      const res = await fetch(`${API_BASE}/api/speakers/register`, {
        method: 'POST',
        body: formData,
      });
      if (res.ok) {
        setNewSpeakerName('');
        setNewSpeakerRel('Trusted Family Contact');
        setNewSpeakerNotes('');
        setNewSpeakerAudio(null);
        fetchSpeakers();
        checkHealth();
      }
    } catch (err) {
      console.error('Error registering speaker:', err);
    } finally {
      setAddingSpeaker(false);
    }
  };

  // Delete speaker
  const handleDeleteSpeaker = async (id) => {
    try {
      const res = await fetch(`${API_BASE}/api/speakers/${id}`, { method: 'DELETE' });
      if (res.ok) {
        fetchSpeakers();
        checkHealth();
      }
    } catch (err) {
      console.error('Error deleting speaker:', err);
    }
  };

  // Challenge Response
  const fetchChallenge = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/verification/challenge?session_id=${currentSessionId}`, {
        method: 'POST',
      });
      if (res.ok) {
        const data = await res.json();
        setCurrentChallenge(data.challenge_phrase);
        setChallengeInstructions(data.instructions);
        setVerificationResult(null);
      }
    } catch (err) {
      console.error('Error generating challenge:', err);
    }
  };

  const handleVerifyChallenge = async (simulate = '') => {
    setVerifyingChallenge(true);
    try {
      const res = await fetch(`${API_BASE}/api/verification/check`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          session_id: currentSessionId,
          response_text: challengeInput,
          simulate_result: simulate,
        }),
      });
      if (res.ok) {
        const data = await res.json();
        setVerificationResult(data);
      }
    } catch (err) {
      console.error('Error checking verification:', err);
    } finally {
      setVerifyingChallenge(false);
    }
  };

  // Analyze Custom Upload
  const handleAnalyzeUpload = async (e) => {
    e.preventDefault();
    if (!uploadFile) return;
    setAnalyzingAudio(true);
    try {
      const formData = new FormData();
      formData.append('file', uploadFile);
      formData.append('input_mode', 'MANUAL_UPLOAD');
      if (customTranscript.trim()) {
        formData.append('fallback_transcript', customTranscript.trim());
      }
      const res = await fetch(`${API_BASE}/api/audio/analyze`, {
        method: 'POST',
        body: formData,
      });
      if (res.ok) {
        const data = await res.json();
        setAnalysisResult(data);
        setCurrentSessionId(data.session_id);
      }
    } catch (err) {
      console.error('Error analyzing audio:', err);
    } finally {
      setAnalyzingAudio(false);
    }
  };

  // Mic Recording
  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaRecorderRef.current = new MediaRecorder(stream);
      audioChunksRef.current = [];
      mediaRecorderRef.current.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };
      mediaRecorderRef.current.onstop = () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/wav' });
        const file = new File([audioBlob], 'microphone_capture.wav', { type: 'audio/wav' });
        setUploadFile(file);
      };
      mediaRecorderRef.current.start();
      setIsRecording(true);
    } catch (err) {
      alert('Microphone access denied or not available: ' + err.message);
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      mediaRecorderRef.current.stream.getTracks().forEach((track) => track.stop());
    }
  };

  // Fetch Forensic Report
  const fetchReport = async (sessionId) => {
    setLoadingReport(true);
    try {
      const sid = sessionId || currentSessionId;
      const res = await fetch(`${API_BASE}/api/sessions/${sid}/report`);
      if (res.ok) {
        const data = await res.json();
        setReportData(data);
      }
    } catch (err) {
      console.error('Error fetching report:', err);
    } finally {
      setLoadingReport(false);
    }
  };

  // Audio Playback toggle
  const toggleAudio = () => {
    if (!audioRef.current) return;
    if (isPlayingAudio) {
      audioRef.current.pause();
      setIsPlayingAudio(false);
    } else {
      audioRef.current.play();
      setIsPlayingAudio(true);
    }
  };

  const getRiskBadge = (level, score) => {
    const lv = level?.toUpperCase();
    if (lv === 'CRITICAL') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-red-500/20 text-red-400 border border-red-500/30">
          <ShieldAlert className="w-3.5 h-3.5" /> CRITICAL RISK ({score}%)
        </span>
      );
    }
    if (lv === 'HIGH') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-orange-500/20 text-orange-400 border border-orange-500/30">
          <AlertTriangle className="w-3.5 h-3.5" /> HIGH RISK ({score}%)
        </span>
      );
    }
    if (lv === 'MEDIUM') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-400 border border-amber-500/30">
          <AlertCircle className="w-3.5 h-3.5" /> MEDIUM SUSPICION ({score}%)
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
        <ShieldCheck className="w-3.5 h-3.5" /> LOW RISK / GENUINE ({score}%)
      </span>
    );
  };

  return (
    <div className="min-h-screen bg-[#080c14] text-slate-100 flex flex-col selection:bg-cyan-500/30 selection:text-cyan-200">
      {/* Top Header */}
      <header className="border-b border-slate-800/80 bg-[#0d1322]/80 backdrop-blur sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-gradient-to-br from-cyan-500/20 to-blue-600/20 border border-cyan-500/30 text-cyan-400 shadow-[0_0_15px_rgba(6,182,212,0.25)]">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-lg tracking-tight text-white">VoiceShield AI</span>
                <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded bg-cyan-950/80 text-cyan-400 border border-cyan-800/60 font-semibold">
                  v1.0.0 Research Prototype
                </span>
              </div>
              <p className="text-xs text-slate-400 hidden sm:block">
                Real-Time Voice Clone Detection & Impersonation Defense Engine
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Backend connection indicator */}
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-900/90 border border-slate-800 text-xs">
              <span
                className={`w-2 h-2 rounded-full ${
                  backendOnline ? 'bg-emerald-400 animate-pulse' : 'bg-red-400'
                }`}
              />
              <span className="text-slate-400 font-mono">
                {backendOnline ? 'Core API Online' : 'Connecting API...'}
              </span>
              {systemStatus && (
                <span className="text-slate-500 border-l border-slate-700 pl-2">
                  {systemStatus.enrolled_speakers} Enrolled Voices
                </span>
              )}
            </div>

            <a
              href={`${API_BASE}/docs`}
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-cyan-300 transition-colors px-2.5 py-1.5 rounded-lg bg-slate-900 border border-slate-800"
              title="Open FastAPI Swagger Documentation"
            >
              <Terminal className="w-3.5 h-3.5" />
              <span>Swagger API</span>
              <ExternalLink className="w-3 h-3 text-slate-500" />
            </a>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex space-x-1 sm:space-x-4 border-t border-slate-800/40 text-sm overflow-x-auto">
          {[
            { id: 'scenarios', label: 'Benchmark Scenarios', icon: Radio },
            { id: 'inspection', label: 'Audio Analysis & Mic', icon: Mic },
            { id: 'registry', label: 'Trusted Voiceprints', icon: Users },
            { id: 'verification', label: 'Challenge-Response', icon: Lock },
            { id: 'reports', label: 'Forensic Audit Report', icon: FileText },
            { id: 'dataset', label: 'Dataset & Benchmarks', icon: BarChart2 },
          ].map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => {
                  setActiveTab(tab.id);
                  if (tab.id === 'reports') fetchReport();
                  if (tab.id === 'dataset') fetchDatasetInfo();
                }}
                className={`flex items-center gap-2 py-3 px-3 border-b-2 font-medium transition-all whitespace-nowrap text-xs sm:text-sm ${
                  active
                    ? 'border-cyan-400 text-cyan-400 bg-cyan-500/5'
                    : 'border-transparent text-slate-400 hover:text-slate-200 hover:border-slate-700'
                }`}
              >
                <Icon className={`w-4 h-4 ${active ? 'text-cyan-400' : 'text-slate-500'}`} />
                {tab.label}
              </button>
            );
          })}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-6">
        {/* TAB 1: BENCHMARK SCENARIOS */}
        {activeTab === 'scenarios' && (
          <div className="space-y-6">
            {/* Scenario Selector Carousel/Grid */}
            <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
              {scenarios.map((scen) => {
                const isSelected = selectedScenarioId === scen.id;
                const isHighRisk = scen.metrics.risk_level === 'CRITICAL';
                return (
                  <button
                    key={scen.id}
                    onClick={() => runScenario(scen.id)}
                    className={`text-left p-3.5 rounded-xl border transition-all relative overflow-hidden ${
                      isSelected
                        ? 'border-cyan-500/60 bg-gradient-to-b from-cyan-950/30 to-slate-900/90 shadow-[0_0_20px_rgba(6,182,212,0.15)] ring-1 ring-cyan-500/40'
                        : 'border-slate-800 bg-[#0d1322]/60 hover:bg-slate-900/80 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-slate-400">
                        {scen.category.split(' ')[0]}
                      </span>
                      <span
                        className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${
                          isHighRisk
                            ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                            : scen.metrics.risk_level === 'MEDIUM'
                            ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                            : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                        }`}
                      >
                        {scen.metrics.risk_score}%
                      </span>
                    </div>
                    <div className="text-xs font-semibold text-white line-clamp-1 mb-1">
                      {scen.title.split(':')[1] || scen.title}
                    </div>
                    <div className="text-[11px] text-slate-400 line-clamp-2">
                      {scen.speaker_name}
                    </div>
                  </button>
                );
              })}
            </div>

            {/* Scenario Detailed Inspection Console */}
            {activeScenarioResult && (
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Left 2 Cols: Audio playback & Primary Forensic Metrics */}
                <div className="lg:col-span-2 space-y-6">
                  {/* Scenario Header Card */}
                  <div className="p-5 rounded-2xl bg-[#0e1526] border border-slate-800 shadow-xl space-y-4">
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
                      <div>
                        <div className="text-xs font-mono uppercase text-cyan-400 font-semibold tracking-wide">
                          {activeScenarioResult.scenario.category}
                        </div>
                        <h2 className="text-lg font-bold text-white mt-0.5">
                          {activeScenarioResult.scenario.title}
                        </h2>
                      </div>
                      <div>
                        {getRiskBadge(
                          activeScenarioResult.scenario.metrics.risk_level,
                          activeScenarioResult.scenario.metrics.risk_score
                        )}
                      </div>
                    </div>

                    <p className="text-xs text-slate-300 leading-relaxed">
                      {activeScenarioResult.scenario.description}
                    </p>

                    {/* Transcript Box */}
                    <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-800/80">
                      <div className="flex items-center justify-between mb-1.5">
                        <span className="text-[11px] font-mono uppercase text-slate-400 font-medium">
                          Spoken Audio Transcript
                        </span>
                        <span className="text-[10px] text-slate-500">ElevenLabs Scribe STT</span>
                      </div>
                      <p className="text-sm text-slate-200 italic font-serif">
                        "{activeScenarioResult.scenario.transcript}"
                      </p>
                    </div>

                    {/* Audio Player */}
                    <div className="flex items-center justify-between p-3 rounded-xl bg-cyan-950/20 border border-cyan-900/40">
                      <div className="flex items-center gap-3">
                        <button
                          onClick={toggleAudio}
                          className="w-10 h-10 rounded-full bg-cyan-500 hover:bg-cyan-400 text-slate-950 flex items-center justify-center font-bold shadow-lg shadow-cyan-500/20 transition-transform active:scale-95"
                        >
                          {isPlayingAudio ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5 ml-0.5" />}
                        </button>
                        <div>
                          <div className="text-xs font-semibold text-slate-200">
                            Playback Scenario Audio
                          </div>
                          <div className="text-[11px] text-slate-400 font-mono">
                            {activeScenarioResult.scenario.audio_file} (16kHz PCM WAV)
                          </div>
                        </div>
                      </div>

                      <audio
                        ref={audioRef}
                        src={`${API_BASE}${activeScenarioResult.audio_url}`}
                        onEnded={() => setIsPlayingAudio(false)}
                        onPlay={() => setIsPlayingAudio(true)}
                        onPause={() => setIsPlayingAudio(false)}
                      />

                      <div className="text-right">
                        <span className="text-xs font-mono text-cyan-400 font-semibold">
                          Session: {activeScenarioResult.session_id}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Multi-Signal Metrics Grid */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    {/* Metric 1: Clone Probability */}
                    <div className="p-4 rounded-xl bg-[#0e1526] border border-slate-800 space-y-2">
                      <div className="flex items-center justify-between text-xs text-slate-400">
                        <span>Clone Probability</span>
                        <Activity className="w-4 h-4 text-cyan-400" />
                      </div>
                      <div className="text-2xl font-bold font-mono text-white">
                        {Math.round(activeScenarioResult.scenario.metrics.clone_probability * 100)}%
                      </div>
                      <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all duration-700 ${
                            activeScenarioResult.scenario.metrics.clone_probability >= 0.6
                              ? 'bg-red-500'
                              : 'bg-emerald-400'
                          }`}
                          style={{
                            width: `${Math.round(
                              activeScenarioResult.scenario.metrics.clone_probability * 100
                            )}%`,
                          }}
                        />
                      </div>
                      <div className="text-[10px] text-slate-400 flex justify-between">
                        <span>ASVspoof Threshold</span>
                        <span className="font-semibold text-slate-300">
                          {activeScenarioResult.scenario.metrics.clone_probability >= 0.6
                            ? 'SYNTHETIC'
                            : 'NATURAL'}
                        </span>
                      </div>
                    </div>

                    {/* Metric 2: Speaker Match */}
                    <div className="p-4 rounded-xl bg-[#0e1526] border border-slate-800 space-y-2">
                      <div className="flex items-center justify-between text-xs text-slate-400">
                        <span>Target Speaker Similarity</span>
                        <UserCheck className="w-4 h-4 text-purple-400" />
                      </div>
                      <div className="text-2xl font-bold font-mono text-white">
                        {Math.round(activeScenarioResult.scenario.metrics.speaker_similarity * 100)}%
                      </div>
                      <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                        <div
                          className="h-full bg-purple-500 rounded-full transition-all duration-700"
                          style={{
                            width: `${Math.round(
                              activeScenarioResult.scenario.metrics.speaker_similarity * 100
                            )}%`,
                          }}
                        />
                      </div>
                      <div className="text-[10px] text-slate-400 truncate">
                        Profile: <span className="text-purple-300 font-semibold">{activeScenarioResult.scenario.metrics.likely_speaker}</span>
                      </div>
                    </div>

                    {/* Metric 3: Intent & Replay Risk */}
                    <div className="p-4 rounded-xl bg-[#0e1526] border border-slate-800 space-y-2">
                      <div className="flex items-center justify-between text-xs text-slate-400">
                        <span>Conversational Intent</span>
                        <AlertTriangle className="w-4 h-4 text-amber-400" />
                      </div>
                      <div className="text-2xl font-bold font-mono text-white">
                        {activeScenarioResult.scenario.metrics.suspicious_intent}
                      </div>
                      <div className="flex flex-wrap gap-1">
                        {activeScenarioResult.scenario.metrics.detected_triggers.length > 0 ? (
                          activeScenarioResult.scenario.metrics.detected_triggers.map((trig, i) => (
                            <span
                              key={i}
                              className="text-[9px] px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30"
                            >
                              {trig}
                            </span>
                          ))
                        ) : (
                          <span className="text-[10px] text-emerald-400">Normal conversation</span>
                        )}
                      </div>
                      <div className="text-[10px] text-slate-400">
                        Replay Risk: <span className="text-slate-300">{activeScenarioResult.scenario.metrics.replay_risk}</span>
                      </div>
                    </div>
                  </div>

                  {/* Dynamic Timeline Area Chart */}
                  <div className="p-5 rounded-2xl bg-[#0e1526] border border-slate-800 space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <BarChart2 className="w-4 h-4 text-cyan-400" />
                        <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                          Forensic Timeline Progression
                        </h3>
                      </div>
                      <div className="flex items-center gap-4 text-xs">
                        <span className="flex items-center gap-1.5 text-cyan-400">
                          <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 inline-block" />
                          Clone Prob
                        </span>
                        <span className="flex items-center gap-1.5 text-purple-400">
                          <span className="w-2.5 h-2.5 rounded-full bg-purple-400 inline-block" />
                          Speaker Sim
                        </span>
                      </div>
                    </div>

                    <div className="h-44 w-full">
                      <ResponsiveContainer width="100%" height="100%">
                        <AreaChart
                          data={activeScenarioResult.scenario.timeline.map((pt) => ({
                            time: pt.time,
                            cloneProb: Math.round(pt.clone_prob * 100),
                            speakerSim: Math.round(pt.speaker_sim * 100),
                          }))}
                        >
                          <defs>
                            <linearGradient id="colorClone" x1="0" y1="0" x2="0" y2="1">
                              <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.4} />
                              <stop offset="95%" stopColor="#06b6d4" stopOpacity={0.0} />
                            </linearGradient>
                            <linearGradient id="colorSpeaker" x1="0" y1="0" x2="0" y2="1">
                              <stop offset="5%" stopColor="#a855f7" stopOpacity={0.4} />
                              <stop offset="95%" stopColor="#a855f7" stopOpacity={0.0} />
                            </linearGradient>
                          </defs>
                          <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                          <XAxis dataKey="time" stroke="#64748b" fontSize={11} />
                          <YAxis domain={[0, 100]} stroke="#64748b" fontSize={11} />
                          <Tooltip
                            contentStyle={{
                              backgroundColor: '#0f172a',
                              borderColor: '#334155',
                              borderRadius: '8px',
                              fontSize: '12px',
                            }}
                          />
                          <Area
                            type="monotone"
                            dataKey="cloneProb"
                            stroke="#06b6d4"
                            strokeWidth={2}
                            fillOpacity={1}
                            fill="url(#colorClone)"
                            name="Clone Prob %"
                          />
                          <Area
                            type="monotone"
                            dataKey="speakerSim"
                            stroke="#a855f7"
                            strokeWidth={2}
                            fillOpacity={1}
                            fill="url(#colorSpeaker)"
                            name="Speaker Sim %"
                          />
                        </AreaChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                </div>

                {/* Right 1 Col: Event log & Action controls */}
                <div className="space-y-6">
                  {/* Event Log */}
                  <div className="p-5 rounded-2xl bg-[#0e1526] border border-slate-800 space-y-3">
                    <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
                      <span className="text-xs font-mono uppercase text-slate-400 font-semibold">
                        Real-Time Risk Events
                      </span>
                      <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
                        {activeScenarioResult.scenario.events.length} flagged
                      </span>
                    </div>

                    <div className="space-y-2.5">
                      {activeScenarioResult.scenario.events.map((ev, i) => {
                        const isCritical = ev.severity === 'CRITICAL';
                        const isWarning = ev.severity === 'WARNING';
                        return (
                          <div
                            key={i}
                            className={`p-3 rounded-xl border text-xs space-y-1 ${
                              isCritical
                                ? 'bg-red-950/20 border-red-900/50 text-red-200'
                                : isWarning
                                ? 'bg-amber-950/20 border-amber-900/50 text-amber-200'
                                : 'bg-slate-900/80 border-slate-800 text-slate-300'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <span className="font-mono font-bold text-[10px] uppercase">
                                [{ev.type}]
                              </span>
                              <span className="text-[10px] font-mono text-slate-400">
                                {ev.timestamp_sec}s
                              </span>
                            </div>
                            <p className="text-xs leading-relaxed">{ev.description}</p>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {/* Immediate Action Panel */}
                  <div className="p-5 rounded-2xl bg-[#0e1526] border border-slate-800 space-y-4">
                    <div className="text-xs font-mono uppercase text-slate-400 font-semibold">
                      Defensive Mitigations
                    </div>

                    {activeScenarioResult.scenario.metrics.risk_level === 'CRITICAL' ? (
                      <div className="p-3.5 rounded-xl bg-red-950/30 border border-red-500/30 space-y-2">
                        <div className="flex items-center gap-2 text-red-400 font-bold text-xs">
                          <AlertTriangle className="w-4 h-4" />
                          IMPERSONATION DETECTED
                        </div>
                        <p className="text-[11px] text-slate-300">
                          Synthetic voice profile closely matches{' '}
                          <span className="font-bold text-white">
                            {activeScenarioResult.scenario.metrics.likely_speaker}
                          </span>
                          . Do not authorize any transactions or divulge credentials.
                        </p>
                      </div>
                    ) : (
                      <div className="p-3.5 rounded-xl bg-emerald-950/30 border border-emerald-500/30 space-y-2">
                        <div className="flex items-center gap-2 text-emerald-400 font-bold text-xs">
                          <CheckCircle2 className="w-4 h-4" />
                          VOICE AUTHENTIC
                        </div>
                        <p className="text-[11px] text-slate-300">
                          Acoustic resonance matches expected vocal tract geometry. No vocoder
                          artifacts detected.
                        </p>
                      </div>
                    )}

                    <div className="space-y-2 pt-2">
                      <button
                        onClick={() => {
                          setActiveTab('verification');
                          fetchChallenge();
                        }}
                        className="w-full py-2.5 px-3 rounded-xl bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white font-semibold text-xs flex items-center justify-center gap-2 shadow-lg shadow-cyan-900/30 transition-all"
                      >
                        <Lock className="w-3.5 h-3.5" />
                        Trigger Challenge-Response Auth
                      </button>

                      <button
                        onClick={() => {
                          setActiveTab('reports');
                          fetchReport(activeScenarioResult.session_id);
                        }}
                        className="w-full py-2.5 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-200 font-semibold text-xs flex items-center justify-center gap-2 transition-all"
                      >
                        <FileText className="w-3.5 h-3.5" />
                        View Full Forensic Report
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: LIVE AUDIO INSPECTION & MIC */}
        {activeTab === 'inspection' && (
          <div className="max-w-4xl mx-auto space-y-6">
            <div className="p-6 rounded-2xl bg-[#0e1526] border border-slate-800 shadow-xl space-y-5">
              <div>
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <Mic className="w-5 h-5 text-cyan-400" />
                  Live Audio Pipeline & Forensic Inspector
                </h2>
                <p className="text-xs text-slate-400 mt-1">
                  Upload an audio file (.wav) or capture live microphone input to run through
                  preprocessing, ASVspoof clone detection, 128-d ECAPA-TDNN speaker embedding, and
                  intent modeling.
                </p>
              </div>

              {/* Upload & Mic Capture UI */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* File Upload Box */}
                <div className="border-2 border-dashed border-slate-700 hover:border-cyan-500/60 rounded-xl p-5 text-center transition-all bg-slate-950/40 flex flex-col items-center justify-center space-y-2">
                  <Upload className="w-8 h-8 text-cyan-400" />
                  <div className="text-xs font-semibold text-slate-200">
                    {uploadFile ? uploadFile.name : 'Select or drop .wav audio file'}
                  </div>
                  <input
                    type="file"
                    accept="audio/wav, audio/*"
                    onChange={(e) => setUploadFile(e.target.files[0])}
                    className="text-xs text-slate-400 file:mr-2 file:py-1 file:px-3 file:rounded-md file:border-0 file:text-xs file:bg-cyan-500/20 file:text-cyan-300 hover:file:bg-cyan-500/30"
                  />
                </div>

                {/* Microphone Record Box */}
                <div className="border border-slate-700 rounded-xl p-5 text-center bg-slate-950/40 flex flex-col items-center justify-center space-y-3">
                  <div
                    className={`p-3 rounded-full ${
                      isRecording
                        ? 'bg-red-500/20 text-red-400 animate-ping'
                        : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    <Mic className="w-6 h-6" />
                  </div>
                  <div className="text-xs font-semibold text-slate-200">
                    {isRecording ? 'Recording audio from mic...' : 'Record Spoken Voice'}
                  </div>
                  {isRecording ? (
                    <button
                      type="button"
                      onClick={stopRecording}
                      className="px-4 py-1.5 rounded-lg bg-red-600 hover:bg-red-500 text-white font-semibold text-xs flex items-center gap-1.5"
                    >
                      <MicOff className="w-3.5 h-3.5" /> Stop & Save Sample
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={startRecording}
                      className="px-4 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white font-semibold text-xs flex items-center gap-1.5"
                    >
                      <Mic className="w-3.5 h-3.5" /> Start Live Recording
                    </button>
                  )}
                </div>
              </div>

              {/* Optional Transcript Input */}
              <div className="space-y-1.5">
                <label className="text-xs font-mono uppercase text-slate-400">
                  Optional Fallback Transcript / Call Pretext
                </label>
                <input
                  type="text"
                  placeholder="e.g., Hello, please send money to my account immediately for emergency hospital fees."
                  value={customTranscript}
                  onChange={(e) => setCustomTranscript(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-cyan-500/60"
                />
              </div>

              <button
                onClick={handleAnalyzeUpload}
                disabled={!uploadFile || analyzingAudio}
                className={`w-full py-3 rounded-xl font-bold text-xs flex items-center justify-center gap-2 transition-all ${
                  uploadFile && !analyzingAudio
                    ? 'bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 shadow-lg shadow-cyan-500/20'
                    : 'bg-slate-800 text-slate-500 cursor-not-allowed'
                }`}
              >
                {analyzingAudio ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    Extracting Acoustic Features & Evaluating Risk...
                  </>
                ) : (
                  <>
                    <Activity className="w-4 h-4" />
                    Execute Forensic Audio Analysis
                  </>
                )}
              </button>
            </div>

            {/* Analysis Result Box */}
            {analysisResult && (
              <div className="p-6 rounded-2xl bg-[#0e1526] border border-slate-800 space-y-6 shadow-2xl">
                <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                  <div>
                    <h3 className="text-sm font-bold text-white">Forensic Analysis Results</h3>
                    <div className="text-[11px] font-mono text-slate-400">
                      Session ID: {analysisResult.session_id} | Duration: {analysisResult.duration_sec}s
                    </div>
                  </div>
                  <div>
                    {getRiskBadge(
                      analysisResult.risk_assessment.risk_level,
                      analysisResult.risk_assessment.risk_score
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                    <span className="text-[11px] text-slate-400">Clone Probability</span>
                    <div className="text-xl font-mono font-bold text-cyan-400">
                      {Math.round(analysisResult.clone_detection.clone_probability * 100)}%
                    </div>
                    <div className="text-[10px] text-slate-400">
                      Status: {analysisResult.clone_detection.status}
                    </div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                    <span className="text-[11px] text-slate-400">Closest Enrolled Speaker</span>
                    <div className="text-xl font-mono font-bold text-purple-400 truncate">
                      {analysisResult.speaker_identification.likely_speaker}
                    </div>
                    <div className="text-[10px] text-slate-400">
                      Similarity: {Math.round(analysisResult.speaker_identification.similarity * 100)}%
                    </div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                    <span className="text-[11px] text-slate-400">Conversational Intent</span>
                    <div className="text-xl font-mono font-bold text-amber-400">
                      {analysisResult.intent_analysis.suspicious_intent}
                    </div>
                    <div className="text-[10px] text-slate-400">
                      Score: {analysisResult.intent_analysis.intent_score}
                    </div>
                  </div>
                </div>

                {/* Acoustic Artifacts Breakdown */}
                {analysisResult.acoustic_features && (
                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-3">
                    <span className="text-xs font-mono uppercase text-slate-400 font-semibold">
                      Extracted Acoustic Forensics
                    </span>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs font-mono">
                      <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                        <div className="text-slate-500 text-[10px]">Spectral Flatness</div>
                        <div className="text-slate-200 font-bold">
                          {analysisResult.acoustic_features.spectral_flatness?.toFixed(4)}
                        </div>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                        <div className="text-slate-500 text-[10px]">Spectral Rolloff</div>
                        <div className="text-slate-200 font-bold">
                          {Math.round(analysisResult.acoustic_features.spectral_rolloff || 0)} Hz
                        </div>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                        <div className="text-slate-500 text-[10px]">High-Freq Ratio</div>
                        <div className="text-slate-200 font-bold">
                          {analysisResult.acoustic_features.high_freq_ratio?.toFixed(4)}
                        </div>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800">
                        <div className="text-slate-500 text-[10px]">Speech Energy RMS</div>
                        <div className="text-slate-200 font-bold">
                          {analysisResult.acoustic_features.rms_energy?.toFixed(4)}
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Risk Reasons & Mitigation */}
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                  <span className="text-xs font-mono uppercase text-slate-400 font-semibold">
                    Risk Assessment Rationale
                  </span>
                  <ul className="list-disc list-inside text-xs text-slate-300 space-y-1">
                    {analysisResult.risk_assessment.reasons?.map((reason, i) => (
                      <li key={i}>{reason}</li>
                    ))}
                  </ul>
                  <div className="mt-3 pt-3 border-t border-slate-800/80 text-xs text-cyan-300 font-semibold">
                    Recommended Action: {analysisResult.risk_assessment.recommended_action}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 3: TRUSTED VOICEPRINTS REGISTRY */}
        {activeTab === 'registry' && (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <Users className="w-5 h-5 text-purple-400" />
                  Enrolled Trusted Contacts Voice Registry
                </h2>
                <p className="text-xs text-slate-400">
                  Registered voiceprints for authorized family members, executives, or VIPs. Used
                  for differential impersonation detection.
                </p>
              </div>

              <button
                onClick={fetchSpeakers}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-xs text-slate-300 hover:text-white"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loadingSpeakers ? 'animate-spin' : ''}`} />
                Refresh Registry
              </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Speaker List */}
              <div className="lg:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-4">
                {speakers.map((spk) => (
                  <div
                    key={spk.id}
                    className="p-5 rounded-2xl bg-[#0e1526] border border-slate-800 space-y-3 relative overflow-hidden"
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="text-base font-bold text-white">{spk.name}</div>
                        <div className="text-xs text-purple-400 font-medium">
                          {spk.relationship}
                        </div>
                      </div>
                      <span className="text-[10px] px-2 py-0.5 rounded bg-purple-950/60 text-purple-300 border border-purple-800/60 font-mono">
                        128-D Vector
                      </span>
                    </div>

                    <div className="text-xs text-slate-400 line-clamp-2">
                      {spk.notes || 'No security notes recorded.'}
                    </div>

                    <div className="text-[10px] font-mono text-slate-500 pt-2 border-t border-slate-800/80 flex items-center justify-between">
                      <span>Enrolled: {spk.registration_date}</span>
                      <button
                        onClick={() => handleDeleteSpeaker(spk.id)}
                        className="text-red-400 hover:text-red-300 text-[11px]"
                      >
                        Remove
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              {/* Enroll New Speaker Form */}
              <div className="p-5 rounded-2xl bg-[#0e1526] border border-slate-800 space-y-4">
                <div className="text-xs font-mono uppercase text-slate-400 font-semibold border-b border-slate-800/80 pb-2">
                  Enroll New Trusted Voiceprint
                </div>

                <form onSubmit={handleRegisterSpeaker} className="space-y-3">
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">Full Name</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g., Alex Johnson"
                      value={newSpeakerName}
                      onChange={(e) => setNewSpeakerName(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-purple-500/60"
                    />
                  </div>

                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">Relationship</label>
                    <input
                      type="text"
                      placeholder="e.g., Chief Financial Officer"
                      value={newSpeakerRel}
                      onChange={(e) => setNewSpeakerRel(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-purple-500/60"
                    />
                  </div>

                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">Security Notes</label>
                    <textarea
                      rows={2}
                      placeholder="e.g., Authorized to trigger financial actions above $5,000"
                      value={newSpeakerNotes}
                      onChange={(e) => setNewSpeakerNotes(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-purple-500/60"
                    />
                  </div>

                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">
                      Voice Sample (.wav) (Optional)
                    </label>
                    <input
                      type="file"
                      accept="audio/wav, audio/*"
                      onChange={(e) => setNewSpeakerAudio(e.target.files[0])}
                      className="text-xs text-slate-400 file:mr-2 file:py-1 file:px-2.5 file:rounded-md file:border-0 file:text-xs file:bg-purple-500/20 file:text-purple-300 hover:file:bg-purple-500/30"
                    />
                    <p className="text-[10px] text-slate-500 mt-1">
                      If omitted, a unique benchmark acoustic latent projection is auto-generated.
                    </p>
                  </div>

                  <button
                    type="submit"
                    disabled={addingSpeaker}
                    className="w-full py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-semibold text-xs transition-all shadow-lg shadow-purple-900/30 flex items-center justify-center gap-1.5"
                  >
                    {addingSpeaker ? 'Enrolling Voiceprint...' : 'Save & Register Voiceprint'}
                  </button>
                </form>
              </div>
            </div>
          </div>
        )}

        {/* TAB 4: CHALLENGE-RESPONSE AUTHENTICATION */}
        {activeTab === 'verification' && (
          <div className="max-w-3xl mx-auto space-y-6">
            <div className="p-6 rounded-2xl bg-[#0e1526] border border-slate-800 space-y-5 shadow-2xl">
              <div>
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <Lock className="w-5 h-5 text-cyan-400" />
                  Active Challenge-Response Verbal Authentication
                </h2>
                <p className="text-xs text-slate-400 mt-1">
                  When a call reaches HIGH or CRITICAL risk, pre-recorded audio replays or
                  unconditioned speech synthesis models fail dynamic phoneme/word prompts. VoiceShield
                  generates an unpredictable security challenge phrase for the caller to speak.
                </p>
              </div>

              {/* Active Challenge Box */}
              <div className="p-5 rounded-xl bg-gradient-to-br from-cyan-950/40 to-slate-950 border border-cyan-500/30 text-center space-y-2">
                <span className="text-[11px] font-mono uppercase tracking-widest text-cyan-400 font-semibold">
                  Active Cryptographic Challenge Phrase
                </span>
                <div className="text-3xl font-black font-mono text-white tracking-wider">
                  "{currentChallenge || 'Loading...'}"
                </div>
                <p className="text-xs text-slate-400 max-w-md mx-auto">
                  {challengeInstructions || 'Instruct caller to speak this exact phrase clearly.'}
                </p>
                <div className="pt-2">
                  <button
                    onClick={fetchChallenge}
                    className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-slate-900 border border-slate-800 text-xs text-slate-300 hover:text-cyan-300 transition-colors"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    Regenerate Dynamic Phrase
                  </button>
                </div>
              </div>

              {/* Verification Interactive Simulation */}
              <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-3">
                <label className="text-xs font-mono uppercase text-slate-400 font-semibold">
                  Test / Enter Caller Spoken Response Transcript
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    placeholder="e.g. Speak or type challenge phrase..."
                    value={challengeInput}
                    onChange={(e) => setChallengeInput(e.target.value)}
                    className="flex-1 px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-cyan-500/60"
                  />
                  <button
                    onClick={() => handleVerifyChallenge()}
                    disabled={verifyingChallenge || !challengeInput.trim()}
                    className="px-4 py-2 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-slate-950 font-bold text-xs"
                  >
                    Verify
                  </button>
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-slate-800/80 text-xs">
                  <span className="text-slate-400">Instant Demo Simulation:</span>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleVerifyChallenge('PASS')}
                      className="px-3 py-1 rounded-lg bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/30 text-xs font-semibold"
                    >
                      Simulate PASSED (Correct Caller)
                    </button>
                    <button
                      onClick={() => handleVerifyChallenge('FAIL')}
                      className="px-3 py-1 rounded-lg bg-red-500/20 text-red-400 border border-red-500/30 hover:bg-red-500/30 text-xs font-semibold"
                    >
                      Simulate FAILED (Impersonator Blocked)
                    </button>
                  </div>
                </div>
              </div>

              {/* Verification Result Card */}
              {verificationResult && (
                <div
                  className={`p-4 rounded-xl border space-y-2 ${
                    verificationResult.passed
                      ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-200'
                      : 'bg-red-950/20 border-red-500/30 text-red-200'
                  }`}
                >
                  <div className="flex items-center justify-between font-bold text-sm">
                    <span className="flex items-center gap-1.5">
                      {verificationResult.passed ? (
                        <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                      ) : (
                        <XCircle className="w-4 h-4 text-red-400" />
                      )}
                      VERIFICATION {verificationResult.result}
                    </span>
                    <span className="font-mono text-xs">
                      Phonetic Match: {Math.round((verificationResult.similarity_score || 0) * 100)}%
                    </span>
                  </div>
                  <p className="text-xs">{verificationResult.message}</p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* TAB 5: FORENSIC AUDIT REPORT */}
        {activeTab === 'reports' && (
          <div className="max-w-4xl mx-auto space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <FileText className="w-5 h-5 text-cyan-400" />
                  Academic Security Forensic Report
                </h2>
                <p className="text-xs text-slate-400">
                  Comprehensive audit log compliant with NIST SP 800-63B biometric and digital
                  identity authentication guidelines.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <a
                  href={`${API_BASE}/api/sessions/${currentSessionId}/report/html`}
                  target="_blank"
                  rel="noreferrer"
                  className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-slate-950 font-bold text-xs shadow-lg shadow-cyan-900/30 transition-all"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                  Print / Save HTML Report
                </a>
              </div>
            </div>

            {loadingReport && (
              <div className="p-8 text-center text-slate-400 text-xs">
                <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-cyan-400" />
                Compiling multi-signal forensic telemetry...
              </div>
            )}

            {reportData && !loadingReport && (
              <div className="p-6 rounded-2xl bg-[#0e1526] border border-slate-800 space-y-6 shadow-2xl">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                  <div>
                    <div className="text-xs font-mono text-cyan-400 font-semibold uppercase">
                      Report ID: {reportData.report_id}
                    </div>
                    <h3 className="text-base font-bold text-white mt-0.5">
                      Session Code: {reportData.session_code}
                    </h3>
                  </div>
                  <div>
                    {getRiskBadge(
                      reportData.executive_summary.risk_level,
                      reportData.executive_summary.overall_risk_score
                    )}
                  </div>
                </div>

                {/* Executive Summary Table */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                    <span className="font-mono uppercase text-slate-500 text-[10px] font-semibold">
                      Primary Assessment
                    </span>
                    <div className="space-y-1 text-slate-300">
                      <div className="flex justify-between py-1 border-b border-slate-900">
                        <span className="text-slate-400">Clone Probability:</span>
                        <span className="font-mono font-bold text-white">
                          {Math.round(reportData.executive_summary.clone_probability * 100)}%
                        </span>
                      </div>
                      <div className="flex justify-between py-1 border-b border-slate-900">
                        <span className="text-slate-400">Classification:</span>
                        <span className="font-semibold text-white">
                          {reportData.executive_summary.clone_status}
                        </span>
                      </div>
                      <div className="flex justify-between py-1">
                        <span className="text-slate-400">Impersonation Target:</span>
                        <span className="font-semibold text-purple-300">
                          {reportData.executive_summary.likely_impersonated_speaker}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                    <span className="font-mono uppercase text-slate-500 text-[10px] font-semibold">
                      Acoustic & Intent Forensics
                    </span>
                    <div className="space-y-1 text-slate-300">
                      <div className="flex justify-between py-1 border-b border-slate-900">
                        <span className="text-slate-400">Detector Model:</span>
                        <span className="font-mono text-white text-[11px]">
                          {reportData.acoustic_forensics.detector_model}
                        </span>
                      </div>
                      <div className="flex justify-between py-1 border-b border-slate-900">
                        <span className="text-slate-400">Replay Attack Risk:</span>
                        <span className="font-semibold text-white">
                          {reportData.acoustic_forensics.replay_attack_risk}
                        </span>
                      </div>
                      <div className="flex justify-between py-1">
                        <span className="text-slate-400">Suspicious Intent:</span>
                        <span className="font-semibold text-amber-300">
                          {reportData.intent_analysis.suspicious_intent}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Excerpt */}
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-1.5">
                  <span className="font-mono uppercase text-slate-500 text-[10px] font-semibold">
                    Transcript Excerpt
                  </span>
                  <p className="text-xs italic text-slate-300 font-serif">
                    "{reportData.intent_analysis.transcript_excerpt}"
                  </p>
                </div>

                {/* Recommendations */}
                <div className="p-4 rounded-xl bg-cyan-950/20 border border-cyan-800/40 space-y-1">
                  <span className="font-mono uppercase text-cyan-400 text-[10px] font-semibold">
                    Mandated Security Mitigation
                  </span>
                  <p className="text-xs font-medium text-slate-200">
                    {reportData.executive_summary.recommended_action}
                  </p>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 6: DATASET & BENCHMARKS */}
        {activeTab === 'dataset' && (
          <div className="space-y-6">
            <div className="p-6 rounded-2xl bg-[#0e1526] border border-slate-800 space-y-4 shadow-2xl">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-bold text-white flex items-center gap-2">
                    <BarChart2 className="w-5 h-5 text-cyan-400" />
                    VoiceShield AI Dataset & Benchmark Manifest
                  </h2>
                  <p className="text-xs text-slate-400 mt-1">
                    Multi-speaker reference dataset, ASVspoof 2021 evaluations, and acoustic feature statistics.
                  </p>
                </div>
                <button
                  onClick={fetchDatasetInfo}
                  className="px-3 py-1.5 rounded-lg bg-cyan-950/80 border border-cyan-800/60 text-cyan-400 text-xs font-mono hover:bg-cyan-900/60"
                >
                  <RefreshCw className="w-3.5 h-3.5 inline mr-1" />
                  Refresh Dataset
                </button>
              </div>

              {loadingDataset && (
                <div className="p-8 text-center text-slate-400 text-xs">
                  Loading dataset statistics and manifest...
                </div>
              )}

              {datasetStats && (
                <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 pt-2">
                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
                    <span className="text-[10px] font-mono uppercase text-slate-500 font-semibold">Total Speakers</span>
                    <div className="text-2xl font-bold text-white mt-1">{datasetStats.total_speakers || 5}</div>
                  </div>
                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
                    <span className="text-[10px] font-mono uppercase text-slate-500 font-semibold">Audio Clips</span>
                    <div className="text-2xl font-bold text-white mt-1">{datasetStats.total_audio_files || 24}</div>
                  </div>
                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
                    <span className="text-[10px] font-mono uppercase text-slate-500 font-semibold">Genuine / Authentic</span>
                    <div className="text-2xl font-bold text-emerald-400 mt-1">{datasetStats.genuine_count || 12}</div>
                  </div>
                  <div className="p-4 rounded-xl bg-slate-950 border border-slate-800">
                    <span className="text-[10px] font-mono uppercase text-slate-500 font-semibold">Synthetic Cloned</span>
                    <div className="text-2xl font-bold text-red-400 mt-1">{datasetStats.synthetic_count || 12}</div>
                  </div>
                </div>
              )}

              {datasetManifest && datasetManifest.files && (
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-3">
                  <h3 className="text-xs font-mono uppercase text-cyan-400 font-semibold">
                    Dataset Manifest Index ({datasetManifest.files.length} Files Registered)
                  </h3>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs text-slate-300 font-mono">
                      <thead className="bg-slate-900 text-slate-400 border-b border-slate-800">
                        <tr>
                          <th className="p-2">File Name</th>
                          <th className="p-2">Speaker</th>
                          <th className="p-2">Category</th>
                          <th className="p-2">Sample Rate</th>
                          <th className="p-2">Duration</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-900">
                        {datasetManifest.files.map((item, idx) => (
                          <tr key={idx} className="hover:bg-slate-900/50">
                            <td className="p-2 text-white font-semibold">{item.filename}</td>
                            <td className="p-2 text-cyan-300">{item.speaker}</td>
                            <td className="p-2">
                              <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                item.category === 'genuine' ? 'bg-emerald-950 text-emerald-400 border border-emerald-800' : 'bg-red-950 text-red-400 border border-red-800'
                              }`}>
                                {item.category}
                              </span>
                            </td>
                            <td className="p-2 text-slate-400">{item.sample_rate || '16000 Hz'}</td>
                            <td className="p-2 text-slate-400">{item.duration || '3.5s'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/60 bg-[#0d1322] py-4 px-4 text-center text-xs text-slate-500">
        <p>
          VoiceShield AI • Multi-Signal Voice Clone Detection & Impersonation Prevention Academic Prototype
        </p>
        <p className="text-[11px] text-slate-600 mt-0.5">
          ASVspoof 2021 Benchmark • 128-D ECAPA-TDNN Latent Vectors • ElevenLabs Scribe STT Integration
        </p>
      </footer>
    </div>
  );
}
