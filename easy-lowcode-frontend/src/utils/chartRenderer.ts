import * as echarts from 'echarts'

/**
 * 根据图表配置和数据构建 ECharts option
 */
export function buildChartOption(
  chartType: string,
  title: string,
  data: any[],
  xField?: string,
  yField?: string,
  groupField?: string,
  customOption?: string,
): echarts.EChartsOption {
  if (!data || data.length === 0) {
    return {
      title: { text: title, textStyle: { color: '#ccc', fontSize: 14 } },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: [] }],
      backgroundColor: 'transparent',
    }
  }

  // 解析自定义选项
  let mergeOption: any = {}
  if (customOption) {
    try { mergeOption = JSON.parse(customOption) } catch {}
  }

  const baseOption = buildBaseOption(chartType, title, data, xField, yField, groupField)

  // 深合并
  return deepMerge(baseOption, mergeOption)
}

function buildBaseOption(
  chartType: string,
  title: string,
  data: any[],
  xField?: string,
  yField?: string,
  groupField?: string,
): echarts.EChartsOption {
  const columns = Object.keys(data[0])
  const x = xField || columns[0]
  const yFields = yField ? yField.split(',').map(s => s.trim()) : [columns[columns.length - 1]]
  const group = groupField

  switch (chartType) {
    case 'bar':
      return buildBarOption(title, data, x, yFields, group)
    case 'line':
      return buildLineOption(title, data, x, yFields, group)
    case 'pie':
      return buildPieOption(title, data, x, yFields[0])
    case 'scatter':
      return buildScatterOption(title, data, x, yFields[0])
    case 'radar':
      return buildRadarOption(title, data, yFields)
    case 'gauge':
      return buildGaugeOption(title, data, yFields[0])
    default:
      return { title: { text: title, textStyle: { color: '#ccc' } } }
  }
}

function buildBarOption(_title: string, data: any[], x: string, yFields: string[], group?: string): any {
  const xData = data.map(d => String(d[x]))
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

  let series: any[]
  if (group) {
    const groups = [...new Set(data.map(d => String(d[group])))]
    series = groups.map((g, idx) => ({
      name: g,
      type: 'bar',
      data: data.filter(d => String(d[group]) === g).map(d => Number(d[yFields[0]]) || 0),
      itemStyle: { color: colors[idx % colors.length] },
    }))
  } else {
    series = yFields.map((yf, idx) => ({
      name: yf,
      type: 'bar',
      data: data.map(d => Number(d[yf]) || 0),
      itemStyle: { color: colors[idx % colors.length] },
    }))
  }

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: series.length > 1 ? { data: series.map(s => s.name), textStyle: { color: '#ccc' } } : undefined,
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: xData, axisLabel: { color: '#aaa' }, axisLine: { lineStyle: { color: '#333' } } },
    yAxis: { type: 'value', axisLabel: { color: '#aaa' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,.1)' } } },
    series,
    backgroundColor: 'transparent',
  }
}

function buildLineOption(_title: string, data: any[], x: string, yFields: string[], group?: string): any {
  const xData = data.map(d => String(d[x]))
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de']

  let series: any[]
  if (group) {
    const groups = [...new Set(data.map(d => String(d[group])))]
    series = groups.map((g, idx) => ({
      name: g,
      type: 'line',
      smooth: true,
      data: data.filter(d => String(d[group]) === g).map(d => Number(d[yFields[0]]) || 0),
      itemStyle: { color: colors[idx % colors.length] },
    }))
  } else {
    series = yFields.map((yf, idx) => ({
      name: yf,
      type: 'line',
      smooth: true,
      data: data.map(d => Number(d[yf]) || 0),
      itemStyle: { color: colors[idx % colors.length] },
    }))
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: series.length > 1 ? { data: series.map(s => s.name), textStyle: { color: '#ccc' } } : undefined,
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: xData, axisLabel: { color: '#aaa' }, axisLine: { lineStyle: { color: '#333' } } },
    yAxis: { type: 'value', axisLabel: { color: '#aaa' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,.1)' } } },
    series,
    backgroundColor: 'transparent',
  }
}

function buildPieOption(_title: string, data: any[], nameField: string, valueField: string): any {
  // 聚合相同 nameField 的数据
  const grouped = new Map<string, number>()
  data.forEach(d => {
    const key = String(d[nameField])
    grouped.set(key, (grouped.get(key) || 0) + (Number(d[valueField]) || 0))
  })

  const pieData = Array.from(grouped.entries()).map(([name, value]) => ({ name, value }))
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      data: pieData.map(d => d.name),
      textStyle: { color: '#ccc' },
    },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: 'transparent', borderWidth: 2 },
      label: { color: '#fff', fontSize: 11 },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: pieData.map((d, idx) => ({ ...d, itemStyle: { color: colors[idx % colors.length] } })),
    }],
    backgroundColor: 'transparent',
  }
}

function buildScatterOption(_title: string, data: any[], xField: string, yField: string): any {
  return {
    tooltip: { trigger: 'item', formatter: (p: any) => `${p.data[0]}, ${p.data[1]}` },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'value',
      name: xField,
      axisLabel: { color: '#aaa' },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,.1)' } },
    },
    yAxis: {
      type: 'value',
      name: yField,
      axisLabel: { color: '#aaa' },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,.1)' } },
    },
    series: [{
      type: 'scatter',
      symbolSize: 12,
      data: data.map(d => [Number(d[xField]) || 0, Number(d[yField]) || 0]),
      itemStyle: { color: '#5470c6' },
    }],
    backgroundColor: 'transparent',
  }
}

function buildRadarOption(_title: string, data: any[], yFields: string[]): any {
  const firstRow = data[0] || {}
  const indicators = Object.keys(firstRow).slice(0, 6).map(key => ({
    name: key,
    max: Math.max(...data.map(d => Math.abs(Number(d[key]) || 0)), 1),
  }))
  const seriesData = yFields.map((yf, idx) => {
    const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de']
    return {
      value: data.map(d => Number(d[yf]) || 0),
      name: yf,
      itemStyle: { color: colors[idx % colors.length] },
    }
  })
  return {
    tooltip: { trigger: 'item' },
    legend: { data: yFields, textStyle: { color: '#ccc' } },
    radar: { indicator: indicators, axisName: { color: '#aaa' } },
    series: [{ type: 'radar', data: seriesData }],
    backgroundColor: 'transparent',
  }
}

function buildGaugeOption(_title: string, data: any[], valueField: string): any {
  const value = data.length > 0 ? (Number(data[data.length - 1][valueField]) || 0) : 0
  return {
    series: [{
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: Math.max(value * 1.2, 100),
      splitNumber: 4,
      itemStyle: { color: '#5470c6' },
      progress: { show: true, width: 18 },
      pointer: { length: '60%' },
      axisLine: { lineStyle: { width: 18, color: [[1, '#e8e8e8']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { distance: 20, color: '#aaa', fontSize: 11 },
      anchor: { show: false },
      detail: { formatter: '{value}', fontSize: 24, offsetCenter: [0, '10%'], color: '#5470c6' },
      data: [{ value, name: valueField }],
    }],
    backgroundColor: 'transparent',
  }
}

function deepMerge(target: any, source: any): any {
  const output = { ...target }
  if (isObject(target) && isObject(source)) {
    Object.keys(source).forEach(key => {
      if (isObject(source[key])) {
        if (!(key in target)) Object.assign(output, { [key]: source[key] })
        else output[key] = deepMerge(target[key], source[key])
      } else {
        Object.assign(output, { [key]: source[key] })
      }
    })
  }
  return output
}

function isObject(item: any): boolean {
  return item && typeof item === 'object' && !Array.isArray(item)
}
